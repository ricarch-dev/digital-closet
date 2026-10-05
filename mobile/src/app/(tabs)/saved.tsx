import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { useCloset } from '@/closet/ClosetProvider';
import type { OutfitRecord } from '@/closet/types';
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

type CollectionFilter = 'Todo' | 'Guardados' | 'Ya los usé';

export default function SavedScreen() {
  const { records, garments, notice, dismissNotice, editRecord, markRecordWorn, removeRecord } = useCloset();
  const [filter, setFilter] = useState<CollectionFilter>('Todo');
  const filteredRecords = records
    .filter((record) => {
      if (filter === 'Guardados') return record.status === 'saved';
      if (filter === 'Ya los usé') return record.status === 'worn';
      return true;
    })
    .sort((first, second) => second.updatedAt - first.updatedAt);

  const openEditor = (record: OutfitRecord) => {
    editRecord(record);
    router.navigate('/(tabs)/ideas');
  };

  return (
    <Page>
      <PageTitle
        title="Tus ideas"
        subtitle="Las combinaciones que guardaste y las que ya usaste."
      />
      <Notice message={notice} onDismiss={dismissNotice} />

      <View style={styles.collectionIntro}>
        <SectionHeading title="Tu colección" detail={`${records.length} ideas en esta sesión`} />
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filters}>
        {(['Todo', 'Guardados', 'Ya los usé'] as const).map((option) => (
          <ChoiceChip
            key={option}
            label={option}
            selected={filter === option}
            onPress={() => setFilter(option)}
          />
        ))}
      </ScrollView>

      {filteredRecords.length > 0 ? (
        <View style={styles.list}>
          {filteredRecords.map((record) => {
            const pieces = record.garmentIds
              .map((id) => garments.find((garment) => garment.id === id))
              .filter((garment) => garment !== undefined);
            return (
              <Surface key={record.id} style={styles.record}>
                <View style={styles.recordHeader}>
                  <View style={styles.statusPill}>
                    <Text style={styles.statusText}>{record.status === 'saved' ? 'Guardado' : 'Ya lo usé'}</Text>
                  </View>
                  <Text style={styles.recordMeta}>{record.occasion} · {record.style}</Text>
                </View>

                <View style={styles.pieceStrip}>
                  {pieces.map((piece) => (
                    <GarmentPhoto key={piece.id} garment={piece} style={styles.recordPhoto} />
                  ))}
                </View>

                <Text style={styles.recordReason}>{record.reason}</Text>
                <View style={styles.recordActions}>
                  <ActionButton
                    label="Ajustar idea"
                    variant="secondary"
                    onPress={() => openEditor(record)}
                    style={styles.recordAction}
                  />
                  {record.status === 'saved' ? (
                    <ActionButton
                      label="Marcar como usado"
                      onPress={() => markRecordWorn(record.id)}
                      style={styles.recordAction}
                    />
                  ) : null}
                </View>
                <ActionButton
                  label="Quitar de mi colección"
                  variant="quiet"
                  onPress={() => removeRecord(record.id)}
                  style={styles.removeButton}
                />
              </Surface>
            );
          })}
        </View>
      ) : (
        <View style={styles.emptyCollection}>
          <View style={styles.emptyMark}>
            <Text style={styles.emptyMarkText}>♡</Text>
          </View>
          <Text accessibilityRole="header" style={styles.emptyTitle}>
            {filter === 'Ya los usé' ? 'Todavía no marcaste una idea como usada' : 'Guarda una idea para encontrarla después'}
          </Text>
          <Text style={styles.emptyDescription}>
            Cuando una combinación te sirva, puedes guardarla, ajustarla o marcarla como usada.
          </Text>
          <ActionButton
            label="Buscar una idea"
            onPress={() => router.navigate('/(tabs)/ideas')}
            style={styles.emptyButton}
          />
        </View>
      )}
    </Page>
  );
}

const styles = StyleSheet.create({
  collectionIntro: { gap: spacing.sm },
  filters: { gap: 8, paddingRight: spacing.md },
  list: { gap: spacing.md },
  record: { gap: spacing.md },
  recordHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.sm },
  statusPill: {
    minHeight: 30,
    justifyContent: 'center',
    paddingHorizontal: 12,
    borderRadius: radius.pill,
    backgroundColor: colors.brandSoft,
  },
  statusText: { color: colors.brand, fontSize: 12, fontWeight: '700' },
  recordMeta: { color: colors.muted, fontSize: 13, fontWeight: '600' },
  pieceStrip: { flexDirection: 'row', gap: 7 },
  recordPhoto: { flex: 1, height: 138, minHeight: 110, borderRadius: radius.small },
  recordReason: { color: colors.ink, fontSize: 14, lineHeight: 21 },
  recordActions: { flexDirection: 'row', gap: 9 },
  recordAction: { flex: 1, paddingHorizontal: 9 },
  removeButton: { alignSelf: 'flex-start', paddingHorizontal: 6 },
  emptyCollection: { alignItems: 'flex-start', gap: 12, paddingTop: spacing.md },
  emptyMark: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.pill,
    backgroundColor: colors.accentSoft,
  },
  emptyMarkText: { color: colors.accent, fontSize: 22 },
  emptyTitle: { color: colors.ink, fontSize: 20, lineHeight: 26, fontWeight: '700' },
  emptyDescription: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  emptyButton: { alignSelf: 'stretch', marginTop: spacing.xs },
});
