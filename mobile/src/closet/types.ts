export const categories = [
  'Superior',
  'Inferior',
  'Vestido',
  'Calzado',
  'Abrigo',
  'Accesorio',
] as const;

export type Category = (typeof categories)[number];

export const categoryLabels: Record<Category, string> = {
  Superior: 'Parte de arriba',
  Inferior: 'Parte de abajo',
  Vestido: 'Vestido o enterizo',
  Calzado: 'Calzado',
  Abrigo: 'Abrigo',
  Accesorio: 'Accesorio',
};

export const styles = ['Casual', 'Minimalista', 'Formal', 'Streetwear'] as const;
export type OutfitStyle = (typeof styles)[number];

export const occasions = ['Diario', 'Salida', 'Trabajo', 'Evento'] as const;
export type Occasion = (typeof occasions)[number];

export const garmentColors = [
  { name: 'Blanco', value: '#F6F6F1' },
  { name: 'Crudo', value: '#D9CBB7' },
  { name: 'Azul', value: '#557997' },
  { name: 'Celeste', value: '#93B8C7' },
  { name: 'Oliva', value: '#687455' },
  { name: 'Grafito', value: '#454B4C' },
  { name: 'Negro', value: '#232624' },
  { name: 'Terracota', value: '#B96147' },
  { name: 'Marrón', value: '#815D45' },
] as const;

export type GarmentColor = (typeof garmentColors)[number]['name'];

export type Garment = {
  id: string;
  name: string;
  category: Category;
  color: GarmentColor;
  imageUri: string;
  styles: OutfitStyle[];
  occasions: Occasion[];
};

export type Outfit = {
  id: string;
  garmentIds: string[];
  occasion: Occasion;
  style: OutfitStyle;
  reason: string;
};

export type OutfitRecord = Outfit & {
  status: 'saved' | 'worn';
  updatedAt: number;
};

export type GarmentInput = Pick<Garment, 'name' | 'category' | 'color' | 'imageUri'>;
