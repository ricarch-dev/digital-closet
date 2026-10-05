import { createContext, useContext, useRef, useState, type PropsWithChildren } from 'react';

import { sampleGarments } from './sample-data';
import type {
  Garment,
  GarmentInput,
  Occasion,
  Outfit,
  OutfitRecord,
  OutfitStyle,
} from './types';

type ClosetContextValue = {
  garments: Garment[];
  records: OutfitRecord[];
  currentOutfit: Outfit | null;
  editingRecordId: string | null;
  notice: string;
  addGarment: (input: GarmentInput) => void;
  updateGarment: (id: string, input: GarmentInput) => void;
  createSuggestion: (occasion: Occasion, style: OutfitStyle) => void;
  swapGarment: (garmentId: string) => void;
  saveCurrent: () => void;
  markCurrentWorn: () => void;
  rejectCurrent: () => void;
  editRecord: (record: OutfitRecord) => void;
  markRecordWorn: (id: string) => void;
  removeRecord: (id: string) => void;
  dismissNotice: () => void;
};

type ScoredOutfit = Outfit & { score: number };

const ClosetContext = createContext<ClosetContextValue | null>(null);

const combinationKey = (ids: string[]) => [...ids].sort().join('|');

function buildSuggestions(
  garments: Garment[],
  occasion: Occasion,
  style: OutfitStyle,
): ScoredOutfit[] {
  const tops = garments.filter((garment) => garment.category === 'Superior');
  const bottoms = garments.filter((garment) => garment.category === 'Inferior');
  const dresses = garments.filter((garment) => garment.category === 'Vestido');
  const shoes = garments.filter((garment) => garment.category === 'Calzado');
  const coats = garments.filter((garment) => garment.category === 'Abrigo');
  const combinations: Garment[][] = [];

  let sequence = 0;
  for (const top of tops) {
    for (const bottom of bottoms) {
      for (const shoe of shoes) {
        const pieces = [top, bottom, shoe];
        const coat = coats.find(
          (item) => item.styles.includes(style) || item.occasions.includes(occasion),
        );
        if (coat && sequence % 4 === 0) pieces.push(coat);
        combinations.push(pieces);
        sequence += 1;
      }
    }
  }

  for (const dress of dresses) {
    for (const shoe of shoes) combinations.push([dress, shoe]);
  }

  return combinations
    .map((pieces, index) => {
      const garmentIds = pieces.map((piece) => piece.id);
      const styleMatches = pieces.filter((piece) => piece.styles.includes(style)).length;
      const occasionMatches = pieces.filter((piece) => piece.occasions.includes(occasion)).length;
      return {
        id: `idea-${combinationKey(garmentIds)}`,
        garmentIds,
        occasion,
        style,
        reason: `Una combinación para ${occasion.toLowerCase()} con una guía ${style.toLowerCase()}, hecha con prendas que ya tienes.`,
        score: styleMatches * 3 + occasionMatches * 2 - index / 1000,
      };
    })
    .sort((first, second) => second.score - first.score);
}

export function ClosetProvider({ children }: PropsWithChildren) {
  const [garments, setGarments] = useState(sampleGarments);
  const [records, setRecords] = useState<OutfitRecord[]>([]);
  const [currentOutfit, setCurrentOutfit] = useState<Outfit | null>(null);
  const [editingRecordId, setEditingRecordId] = useState<string | null>(null);
  const [rejectedKeys, setRejectedKeys] = useState<string[]>([]);
  const [notice, setNotice] = useState('');
  const sequence = useRef(0);

  const addGarment = (input: GarmentInput) => {
    const id = `added-${Date.now()}-${sequence.current++}`;
    setGarments((current) => [
      {
        ...input,
        id,
        styles: ['Casual', 'Minimalista'],
        occasions: ['Diario', 'Salida'],
      },
      ...current,
    ]);
    setNotice('Prenda agregada a tu clóset.');
  };

  const updateGarment = (id: string, input: GarmentInput) => {
    setGarments((current) =>
      current.map((garment) => (garment.id === id ? { ...garment, ...input } : garment)),
    );
    setNotice('Prenda actualizada.');
  };

  const createSuggestion = (occasion: Occasion, style: OutfitStyle) => {
    const blocked = new Set(rejectedKeys);
    const currentKey = currentOutfit ? combinationKey(currentOutfit.garmentIds) : null;
    const available = buildSuggestions(garments, occasion, style).filter((suggestion) => {
      const key = combinationKey(suggestion.garmentIds);
      return !blocked.has(key) && key !== currentKey;
    });

    if (available.length === 0) {
      setCurrentOutfit(null);
      setEditingRecordId(null);
      setNotice('No encontramos otra combinación con estas prendas. Prueba otra guía o agrega una prenda.');
      return;
    }

    const selected = available[sequence.current % available.length];
    sequence.current += 1;
    setCurrentOutfit(selected);
    setEditingRecordId(null);
    setNotice('Listo, armamos una idea con prendas de tu clóset.');
  };

  const swapGarment = (garmentId: string) => {
    if (!currentOutfit) return;
    const selectedGarment = garments.find((garment) => garment.id === garmentId);
    if (!selectedGarment) return;

    const categoryItems = garments.filter((garment) => garment.category === selectedGarment.category);
    const currentIndex = categoryItems.findIndex((garment) => garment.id === garmentId);
    const next = Array.from({ length: categoryItems.length - 1 }, (_, index) =>
      categoryItems[(currentIndex + index + 1) % categoryItems.length],
    ).find((garment) => !currentOutfit.garmentIds.includes(garment.id));

    if (!next) {
      setNotice(`No hay otra prenda de ${selectedGarment.category.toLowerCase()} para cambiar.`);
      return;
    }

    const updatedIds = currentOutfit.garmentIds.map((id) => (id === garmentId ? next.id : id));
    setCurrentOutfit({ ...currentOutfit, garmentIds: updatedIds });
    setNotice(`${selectedGarment.category} cambiada por ${next.name.toLowerCase()}.`);
  };

  const saveCurrent = () => {
    if (!currentOutfit) return;
    const existing = records.find((record) => record.id === editingRecordId);

    if (existing) {
      setRecords((current) =>
        current.map((record) =>
          record.id === existing.id
            ? { ...currentOutfit, id: existing.id, status: existing.status, updatedAt: Date.now() }
            : record,
        ),
      );
      setNotice('Cambios guardados.');
    } else {
      const record = {
        ...currentOutfit,
        id: `outfit-${Date.now()}-${sequence.current++}`,
        status: 'saved' as const,
        updatedAt: Date.now(),
      };
      setRecords((current) => [record, ...current]);
      setCurrentOutfit(record);
      setEditingRecordId(record.id);
      setNotice('Idea guardada para volver a encontrarla.');
    }
  };

  const markCurrentWorn = () => {
    if (!currentOutfit) return;
    const existing = records.find((record) => record.id === editingRecordId);
    if (existing) {
      setRecords((current) =>
        current.map((record) =>
          record.id === existing.id
            ? { ...currentOutfit, id: existing.id, status: 'worn', updatedAt: Date.now() }
            : record,
        ),
      );
    } else {
      const record = {
        ...currentOutfit,
        id: `outfit-${Date.now()}-${sequence.current++}`,
        status: 'worn' as const,
        updatedAt: Date.now(),
      };
      setRecords((current) => [record, ...current]);
      setCurrentOutfit(record);
      setEditingRecordId(record.id);
    }
    setNotice('Anotado. Podés encontrarlo en “Ya los usé”.');
  };

  const rejectCurrent = () => {
    if (!currentOutfit) return;
    const key = combinationKey(currentOutfit.garmentIds);
    setRejectedKeys((current) => (current.includes(key) ? current : [...current, key]));
    if (editingRecordId) {
      setRecords((current) => current.filter((record) => record.id !== editingRecordId));
    }
    setCurrentOutfit(null);
    setEditingRecordId(null);
    setNotice('No volveremos a sugerir esta combinación. Tus prendas siguen disponibles.');
  };

  const editRecord = (record: OutfitRecord) => {
    setCurrentOutfit({
      id: record.id,
      garmentIds: [...record.garmentIds],
      occasion: record.occasion,
      style: record.style,
      reason: record.reason,
    });
    setEditingRecordId(record.id);
    setNotice('Podés cambiar una prenda y guardar los ajustes.');
  };

  const markRecordWorn = (id: string) => {
    setRecords((current) =>
      current.map((record) =>
        record.id === id ? { ...record, status: 'worn', updatedAt: Date.now() } : record,
      ),
    );
    setNotice('Anotado en “Ya los usé”.');
  };

  const removeRecord = (id: string) => {
    setRecords((current) => current.filter((record) => record.id !== id));
    setNotice('Idea quitada de tu colección.');
  };

  const value: ClosetContextValue = {
    garments,
    records,
    currentOutfit,
    editingRecordId,
    notice,
    addGarment,
    updateGarment,
    createSuggestion,
    swapGarment,
    saveCurrent,
    markCurrentWorn,
    rejectCurrent,
    editRecord,
    markRecordWorn,
    removeRecord,
    dismissNotice: () => setNotice(''),
  };

  return <ClosetContext.Provider value={value}>{children}</ClosetContext.Provider>;
}

export function useCloset() {
  const context = useContext(ClosetContext);
  if (!context) throw new Error('useCloset must be used inside ClosetProvider.');
  return context;
}
