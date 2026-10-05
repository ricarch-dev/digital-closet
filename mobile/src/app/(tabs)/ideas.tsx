import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useCloset } from '@/closet/ClosetProvider';
import { occasions, styles as outfitStyles, type Occasion, type OutfitStyle } from '@/closet/types';
import {
  ActionButton,
  ChoiceChip,
  GarmentPhoto,
  Notice,
  Page,
  PageTitle,
  SectionHeading,
  Surface,
} from '@/components/ui';
import { colors, radius, spacing } from '@/theme/tokens';

export default function IdeasScreen() {
  const {
    garments,
    currentOutfit,
    editingRecordId,
    notice,
    createSuggestion,
    swapGarment,
    saveCurrent,
    markCurrentWorn,
    rejectCurrent,
    dismissNotice,
  } = useCloset();
  const [occasion, setOccasion] = useState<Occasion>('Diario');
  const [style, setStyle] = useState<OutfitStyle>('Casual');

  const currentGarments = currentOutfit
    ? currentOutfit.garmentIds
        .map((id) => garments.find((garment) => garment.id === id))
        .filter((garment) => garment !== undefined)
    : [];

  return (
    <Page>
      <PageTitle
        title="Ideas para hoy"
        subtitle="Elegí una ocasión y un estilo. La combinación se arma con tu ropa."
      />
      <Notice message={notice} onDismiss={dismissNotice} />

      <Surface style={styles.selector}>
        <View style={styles.choiceGroup}>
          <SectionHeading title="¿Para qué ocasión?" />
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
            {occasions.map((option) => (
              <ChoiceChip
                key={option}
                label={option}
                selected={occasion === option}
                onPress={() => setOccasion(option)}
              />
            ))}
          </ScrollView>
        </View>

        <View style={styles.choiceGroup}>
          <SectionHeading title="¿Qué estilo te gustaría probar?" />
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
            {outfitStyles.map((option) => (
              <ChoiceChip
                key={option}
                label={option}
                selected={style === option}
                onPress={() => setStyle(option)}
              />
            ))}
          </ScrollView>
        </View>

        <Text style={styles.selectionNote}>Tomamos tu elección como guía, tú decides si te gusta.</Text>
        <ActionButton label="Armar una combinación" onPress={() => createSuggestion(occasion, style)} />
      </Surface>

      {currentOutfit ? (
        <View style={styles.outfitSection}>
          <SectionHeading title="Una combinación para ti" detail="Puedes cambiar cualquiera de las prendas." />
          <Surface style={styles.outfitSurface}>
            <Text style={styles.outfitOccasion}>{currentOutfit.occasion} · {currentOutfit.style}</Text>
            <Text style={styles.reason}>{currentOutfit.reason}</Text>
            <View style={styles.pieceList}>
              {currentGarments.map((garment) => (
                <View key={garment.id} style={styles.pieceRow}>
                  <GarmentPhoto garment={garment} style={styles.piecePhoto} />
                  <View style={styles.pieceCopy}>
                    <Text style={styles.pieceCategory}>{garment.category}</Text>
                    <Text style={styles.pieceName}>{garment.name}</Text>
                    <Text style={styles.pieceColor}>{garment.color}</Text>
                  </View>
                  <ActionButton
                    label="Cambiar"
                    accessibilityLabel={`Cambiar ${garment.category.toLowerCase()}`}
                    variant="secondary"
                    onPress={() => swapGarment(garment.id)}
                    style={styles.swapButton}
                  />
                </View>
              ))}
            </View>
            <View style={styles.actionGroup}>
              <ActionButton
                label={editingRecordId ? 'Guardar cambios' : 'Guardar idea'}
                onPress={saveCurrent}
                style={styles.actionButton}
              />
              <ActionButton
                label="Ya lo usé"
                variant="secondary"
                onPress={markCurrentWorn}
                style={styles.actionButton}
              />
            </View>
            <ActionButton
              label="No sugerir esta combinación otra vez"
              variant="quiet"
              onPress={rejectCurrent}
              style={styles.rejectButton}
            />
          </Surface>
        </View>
      ) : (
        <View style={styles.emptyIdea}>
          <View style={styles.emptyIcon}>
            <Text style={styles.emptyIconText}>✦</Text>
          </View>
          <Text accessibilityRole="header" style={styles.emptyTitle}>
            Una idea empieza con tus prendas
          </Text>
          <Text style={styles.emptyDescription}>
            Cuando quieras, pedí una combinación. Si ya descartaste algunas, esas piezas siguen disponibles para otras ideas.
          </Text>
          <ActionButton
            label="Armar una combinación"
            onPress={() => createSuggestion(occasion, style)}
            disabled={garments.length < 2}
            style={styles.emptyButton}
          />
        </View>
      )}
    </Page>
  );
}

const styles = StyleSheet.create({
  selector: { gap: spacing.lg },
  choiceGroup: { gap: spacing.sm },
  chips: { gap: 8, paddingVertical: 2, paddingRight: spacing.md },
  selectionNote: { color: colors.muted, fontSize: 13, lineHeight: 19 },
  outfitSection: { gap: spacing.md },
  outfitSurface: { gap: spacing.md },
  outfitOccasion: { color: colors.brand, fontSize: 14, fontWeight: '700' },
  reason: { color: colors.ink, fontSize: 15, lineHeight: 22 },
  pieceList: { gap: 10 },
  pieceRow: {
    minHeight: 102,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 11,
    padding: 9,
    borderRadius: radius.medium,
    backgroundColor: colors.canvas,
  },
  piecePhoto: { width: 74, height: 84, minHeight: 84, borderRadius: radius.small },
  pieceCopy: { flex: 1, gap: 3 },
  pieceCategory: { color: colors.muted, fontSize: 12, lineHeight: 16 },
  pieceName: { color: colors.ink, fontSize: 14, lineHeight: 18, fontWeight: '700' },
  pieceColor: { color: colors.muted, fontSize: 13, lineHeight: 17 },
  swapButton: { minHeight: 42, paddingHorizontal: 13 },
  actionGroup: { flexDirection: 'row', gap: 9 },
  actionButton: { flex: 1, paddingHorizontal: 10 },
  rejectButton: { alignSelf: 'center', paddingHorizontal: 8 },
  emptyIdea: { alignItems: 'flex-start', gap: 11, paddingTop: spacing.xs },
  emptyIcon: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    backgroundColor: colors.accentSoft,
  },
  emptyIconText: { color: colors.accent, fontSize: 22 },
  emptyTitle: { color: colors.ink, fontSize: 20, lineHeight: 26, fontWeight: '700' },
  emptyDescription: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  emptyButton: { alignSelf: 'stretch', marginTop: 3 },
});
