import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { useCloset } from '@/closet/ClosetProvider';
import { categoryLabels, categories, garmentColors, type Category } from '@/closet/types';
import { ActionButton, ChoiceChip, GarmentPhoto, Notice, Page, SectionHeading } from '@/components/ui';
import { colors, radius, spacing } from '@/theme/tokens';

export default function ClosetScreen() {
  const { garments, notice, dismissNotice } = useCloset();
  const [categoryFilter, setCategoryFilter] = useState<Category | null>(null);
  const visibleGarments = categoryFilter
    ? garments.filter((garment) => garment.category === categoryFilter)
    : garments;

  return (
    <Page>
      <View style={styles.brandRow}>
        <View style={styles.brandMark}>
          <Text style={styles.brandMarkText}>D</Text>
        </View>
        <View style={styles.brandCopy}>
          <Text style={styles.brandName}>Digital Closet</Text>
          <Text style={styles.brandCaption}>Tu ropa, tus ideas</Text>
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Agregar una prenda"
          hitSlop={8}
          onPress={() => router.push('/add-garment')}
          style={styles.addButton}>
          <Text style={styles.addButtonText}>＋</Text>
        </Pressable>
      </View>

      <View style={styles.intro}>
        <Text accessibilityRole="header" style={styles.title}>
          Tu clóset, a mano
        </Text>
        <Text style={styles.subtitle}>
          Encuentra una combinación con prendas que ya tienes.
        </Text>
      </View>

      <View style={styles.ideaPanel}>
        <View style={styles.ideaMark}>
          <Text style={styles.ideaMarkText}>✦</Text>
        </View>
        <View style={styles.ideaCopy}>
          <Text style={styles.ideaTitle}>¿Qué te pones hoy?</Text>
          <Text style={styles.ideaBody}>Elige una ocasión y te proponemos una idea para empezar.</Text>
        </View>
        <ActionButton
          label="Buscar una idea"
          variant="secondary"
          onPress={() => router.navigate('/(tabs)/ideas')}
          style={styles.ideaButton}
        />
      </View>

      <Notice message={notice} onDismiss={dismissNotice} />

      <View style={styles.sectionTop}>
        <SectionHeading title="Tus prendas" detail={`${garments.length} prendas para combinar`} />
        <ActionButton
          label="Agregar"
          variant="quiet"
          onPress={() => router.push('/add-garment')}
          style={styles.addTextButton}
        />
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.filterRow}
        accessibilityLabel="Filtrar prendas por categoría">
        <ChoiceChip label="Todas" selected={categoryFilter === null} onPress={() => setCategoryFilter(null)} />
        {categories.map((category) => (
          <ChoiceChip
            key={category}
            label={categoryLabels[category]}
            selected={categoryFilter === category}
            onPress={() => setCategoryFilter(category)}
          />
        ))}
      </ScrollView>

      {visibleGarments.length > 0 ? (
        <View style={styles.garmentGrid}>
          {visibleGarments.map((garment) => {
            const swatch = garmentColors.find((option) => option.name === garment.color)?.value;
            return (
              <View key={garment.id} style={styles.garmentCard}>
                <GarmentPhoto garment={garment} style={styles.garmentPhoto} />
                <View style={styles.garmentDetails}>
                  <Text numberOfLines={1} style={styles.garmentName}>
                    {garment.name}
                  </Text>
                  <View style={styles.garmentMeta}>
                    <View style={[styles.colorDot, { backgroundColor: swatch ?? colors.line }]} />
                    <Text numberOfLines={1} style={styles.garmentMetaText}>
                      {garment.color} · {categoryLabels[garment.category]}
                    </Text>
                  </View>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Editar ${garment.name}`}
                    hitSlop={5}
                    onPress={() => router.push({ pathname: '/add-garment', params: { garmentId: garment.id } })}
                    style={styles.editGarment}>
                    <Text style={styles.editGarmentText}>Editar prenda</Text>
                  </Pressable>
                </View>
              </View>
            );
          })}
        </View>
      ) : (
        <View style={styles.emptyFilter}>
          <Text style={styles.emptyText}>Todavía no hay prendas en esta categoría.</Text>
          <ActionButton label="Agregar una prenda" onPress={() => router.push('/add-garment')} />
        </View>
      )}

      <Text style={styles.photoNote}>Las fotos que ves son de muestra.</Text>
    </Page>
  );
}

const styles = StyleSheet.create({
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: 11 },
  brandMark: {
    width: 42,
    height: 42,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.brand,
  },
  brandMarkText: { color: colors.white, fontSize: 20, fontWeight: '800' },
  brandCopy: { flex: 1, gap: 1 },
  brandName: { color: colors.ink, fontSize: 15, fontWeight: '800' },
  brandCaption: { color: colors.muted, fontSize: 12 },
  addButton: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    backgroundColor: colors.brandSoft,
  },
  addButtonText: { color: colors.brand, fontSize: 28, lineHeight: 32, fontWeight: '400' },
  intro: { gap: 7, paddingTop: spacing.xs },
  title: { color: colors.ink, fontSize: 32, lineHeight: 38, fontWeight: '700', letterSpacing: -0.7 },
  subtitle: { maxWidth: 540, color: colors.muted, fontSize: 15, lineHeight: 22 },
  ideaPanel: {
    alignItems: 'flex-start',
    gap: 14,
    padding: spacing.md,
    borderRadius: radius.large,
    backgroundColor: colors.brand,
  },
  ideaMark: {
    width: 38,
    height: 38,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    backgroundColor: 'rgba(255,255,255,0.16)',
  },
  ideaMarkText: { color: colors.white, fontSize: 21 },
  ideaCopy: { gap: 5 },
  ideaTitle: { color: colors.white, fontSize: 21, lineHeight: 27, fontWeight: '700' },
  ideaBody: { color: '#E4F1E9', fontSize: 14, lineHeight: 20 },
  ideaButton: { alignSelf: 'stretch', minHeight: 48, borderColor: colors.white },
  sectionTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.sm },
  addTextButton: { paddingHorizontal: 8 },
  filterRow: { gap: 8, paddingRight: spacing.md },
  garmentGrid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', rowGap: 13 },
  garmentCard: {
    width: '48.2%',
    overflow: 'hidden',
    borderRadius: radius.large,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.surface,
  },
  garmentPhoto: { height: 176, borderRadius: 0, minHeight: 130 },
  garmentDetails: { gap: 7, paddingHorizontal: 11, paddingTop: 10, paddingBottom: 11 },
  garmentName: { color: colors.ink, fontSize: 14, lineHeight: 18, fontWeight: '700' },
  garmentMeta: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  colorDot: { width: 11, height: 11, borderRadius: 6, borderWidth: 1, borderColor: colors.line },
  garmentMetaText: { flex: 1, color: colors.muted, fontSize: 11, lineHeight: 15 },
  editGarment: { alignSelf: 'flex-start', minHeight: 36, justifyContent: 'center', paddingRight: 6 },
  editGarmentText: { color: colors.brand, fontSize: 13, fontWeight: '700' },
  emptyFilter: { alignItems: 'flex-start', gap: spacing.md, paddingVertical: spacing.lg },
  emptyText: { color: colors.muted, fontSize: 15, lineHeight: 22 },
  photoNote: { color: colors.muted, fontSize: 12, lineHeight: 18, textAlign: 'center' },
});
