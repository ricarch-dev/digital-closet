import * as ImagePicker from 'expo-image-picker';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { useCloset } from '@/closet/ClosetProvider';
import {
  categories,
  categoryLabels,
  garmentColors,
  type Category,
  type Garment,
  type GarmentColor,
} from '@/closet/types';
import { ActionButton, ChoiceChip, GarmentPhoto, Notice, Page, PageTitle } from '@/components/ui';
import { colors, radius, spacing } from '@/theme/tokens';

export default function AddGarmentScreen() {
  const { garmentId: garmentIdParam } = useLocalSearchParams<{ garmentId?: string | string[] }>();
  const garmentId = Array.isArray(garmentIdParam) ? garmentIdParam[0] : garmentIdParam;
  const { garments } = useCloset();
  const existing = garments.find((garment) => garment.id === garmentId);

  return <GarmentForm key={existing?.id ?? 'new'} existing={existing} />;
}

function GarmentForm({ existing }: { existing: Garment | undefined }) {
  const { addGarment, updateGarment } = useCloset();
  const [name, setName] = useState(existing?.name ?? '');
  const [category, setCategory] = useState<Category | null>(existing?.category ?? null);
  const [color, setColor] = useState<GarmentColor | null>(existing?.color ?? null);
  const [photoUri, setPhotoUri] = useState<string | null>(existing?.imageUri ?? null);
  const [pickerBusy, setPickerBusy] = useState(false);
  const [formMessage, setFormMessage] = useState('');
  const editing = Boolean(existing);

  const choosePhoto = async () => {
    setPickerBusy(true);
    setFormMessage('');
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: true,
        aspect: [4, 5],
        quality: 1,
      });

      if (result.canceled || !result.assets?.[0]) {
        setFormMessage('No se seleccionó ninguna foto. Podés volver a elegirla cuando quieras.');
        return;
      }
      setPhotoUri(result.assets[0].uri);
    } catch {
      setFormMessage('No pudimos abrir tus fotos. Probá otra vez.');
    } finally {
      setPickerBusy(false);
    }
  };

  const saveGarment = () => {
    if (!photoUri || !category || !color) {
      setFormMessage('Elegí una foto, una categoría y un color para guardar la prenda.');
      return;
    }

    const input = {
      name: name.trim() || `${categoryLabels[category]} ${color.toLowerCase()}`,
      category,
      color,
      imageUri: photoUri,
    };

    if (existing) updateGarment(existing.id, input);
    else addGarment(input);
    router.back();
  };

  return (
    <Page>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Volver al clóset"
        hitSlop={8}
        onPress={() => router.back()}
        style={styles.backButton}>
        <Text style={styles.backArrow}>‹</Text>
        <Text style={styles.backText}>Volver al clóset</Text>
      </Pressable>

      <PageTitle
        title={editing ? 'Editar prenda' : 'Sumar una prenda'}
        subtitle="Con una foto, una categoría y un color ya alcanza."
      />

      <View style={styles.photoGroup}>
        <Text style={styles.fieldTitle}>Foto de la prenda</Text>
        {photoUri ? (
          <GarmentPhoto
            garment={{
              name: name.trim() || 'Prenda seleccionada',
              category: category ?? 'Superior',
              color: color ?? 'Blanco',
              imageUri: photoUri,
            }}
            style={styles.photoPreview}
          />
        ) : (
          <View style={styles.photoPlaceholder}>
            <View style={styles.photoPlaceholderMark}>
              <Text style={styles.photoPlaceholderGlyph}>＋</Text>
            </View>
            <Text style={styles.photoPlaceholderTitle}>Una foto ayuda a reconocerla</Text>
            <Text style={styles.photoPlaceholderCopy}>Elegí una imagen de tu dispositivo.</Text>
          </View>
        )}
        <ActionButton
          label={pickerBusy ? 'Abriendo tus fotos…' : photoUri ? 'Cambiar foto' : 'Elegir foto'}
          variant="secondary"
          disabled={pickerBusy}
          onPress={choosePhoto}
          style={styles.photoAction}
        />
      </View>

      <View style={styles.fieldGroup}>
        <Text style={styles.fieldTitle}>Categoría</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.optionRow}>
          {categories.map((option) => (
            <ChoiceChip
              key={option}
              label={categoryLabels[option]}
              selected={category === option}
              onPress={() => setCategory(option)}
            />
          ))}
        </ScrollView>
      </View>

      <View style={styles.fieldGroup}>
        <Text style={styles.fieldTitle}>Color principal</Text>
        <View style={styles.colorGrid}>
          {garmentColors.map((option) => {
            const selected = color === option.name;
            return (
              <Pressable
                key={option.name}
                accessibilityRole="button"
                accessibilityLabel={`Color ${option.name}`}
                accessibilityState={{ selected }}
                hitSlop={4}
                onPress={() => setColor(option.name)}
                style={[styles.colorOption, selected && styles.colorOptionSelected]}>
                <View style={[styles.colorSwatch, { backgroundColor: option.value }]}>
                  {selected ? <Text style={styles.checkmark}>✓</Text> : null}
                </View>
                <Text style={[styles.colorLabel, selected && styles.colorLabelSelected]}>{option.name}</Text>
              </Pressable>
            );
          })}
        </View>
      </View>

      <View style={styles.fieldGroup}>
        <Text style={styles.fieldTitle}>Nombre (opcional)</Text>
        <TextInput
          accessibilityLabel="Nombre opcional de la prenda"
          value={name}
          onChangeText={setName}
          placeholder="Por ejemplo, camisa liviana"
          placeholderTextColor={colors.muted}
          returnKeyType="done"
          maxLength={48}
          style={styles.input}
        />
        <Text style={styles.helperText}>Podés dejarlo en blanco y completarlo después.</Text>
      </View>

      <Notice message={formMessage} onDismiss={() => setFormMessage('')} />
      <ActionButton
        label={editing ? 'Guardar cambios' : 'Agregar al clóset'}
        onPress={saveGarment}
        style={styles.submitButton}
      />
    </Page>
  );
}

const styles = StyleSheet.create({
  backButton: { minHeight: 42, flexDirection: 'row', alignItems: 'center', gap: 8, alignSelf: 'flex-start' },
  backArrow: { color: colors.brand, fontSize: 28, lineHeight: 32 },
  backText: { color: colors.brand, fontSize: 14, fontWeight: '700' },
  photoGroup: { gap: 10 },
  fieldGroup: { gap: 10 },
  fieldTitle: { color: colors.ink, fontSize: 15, lineHeight: 20, fontWeight: '700' },
  photoPreview: { height: 230, minHeight: 170, borderRadius: radius.large },
  photoPlaceholder: {
    minHeight: 210,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    padding: spacing.lg,
    borderRadius: radius.large,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.surface,
  },
  photoPlaceholderMark: {
    width: 46,
    height: 46,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    backgroundColor: colors.brandSoft,
  },
  photoPlaceholderGlyph: { color: colors.brand, fontSize: 26, lineHeight: 30 },
  photoPlaceholderTitle: { color: colors.ink, fontSize: 15, fontWeight: '700', textAlign: 'center' },
  photoPlaceholderCopy: { color: colors.muted, fontSize: 13, textAlign: 'center' },
  photoAction: { alignSelf: 'stretch' },
  optionRow: { gap: 8, paddingRight: spacing.md },
  colorGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 9 },
  colorOption: {
    minWidth: 72,
    minHeight: 72,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 5,
    paddingHorizontal: 8,
    borderRadius: radius.medium,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.surface,
  },
  colorOptionSelected: { borderColor: colors.brand, backgroundColor: colors.brandSoft },
  colorSwatch: {
    width: 25,
    height: 25,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.line,
  },
  checkmark: { color: colors.white, fontSize: 14, fontWeight: '800', textShadowColor: colors.ink, textShadowRadius: 3 },
  colorLabel: { color: colors.muted, fontSize: 12, fontWeight: '600' },
  colorLabelSelected: { color: colors.brand },
  input: {
    minHeight: 50,
    paddingHorizontal: 14,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: radius.medium,
    backgroundColor: colors.surface,
    color: colors.ink,
    fontSize: 15,
  },
  helperText: { color: colors.muted, fontSize: 12, lineHeight: 18 },
  submitButton: { marginTop: spacing.xs },
});
