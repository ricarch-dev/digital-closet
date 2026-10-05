import { useState, type PropsWithChildren, type ReactNode } from 'react';
import {
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type ImageStyle,
  type StyleProp,
  type ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { categoryLabels, garmentColors, type Category, type Garment } from '@/closet/types';
import { colors, radius, spacing } from '@/theme/tokens';

export function Page({ children }: PropsWithChildren) {
  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.pageContent}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}>
        <View style={styles.content}>{children}</View>
      </ScrollView>
    </SafeAreaView>
  );
}

export function PageTitle({
  title,
  subtitle,
  trailing,
}: {
  title: string;
  subtitle?: string;
  trailing?: ReactNode;
}) {
  return (
    <View style={styles.pageTitleRow}>
      <View style={styles.pageTitleCopy}>
        <Text accessibilityRole="header" style={styles.pageTitle}>
          {title}
        </Text>
        {subtitle ? <Text style={styles.pageSubtitle}>{subtitle}</Text> : null}
      </View>
      {trailing}
    </View>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'quiet' | 'danger';

export function ActionButton({
  label,
  onPress,
  variant = 'primary',
  disabled = false,
  style,
  accessibilityHint,
  accessibilityLabel,
}: {
  label: string;
  onPress: () => void;
  variant?: ButtonVariant;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  accessibilityHint?: string;
  accessibilityLabel?: string;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? label}
      accessibilityHint={accessibilityHint}
      accessibilityState={{ disabled }}
      disabled={disabled}
      hitSlop={6}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        styles[`button_${variant}`],
        disabled && styles.buttonDisabled,
        pressed && !disabled && styles.buttonPressed,
        style,
      ]}>
      <Text
        allowFontScaling
        style={[styles.buttonText, styles[`buttonText_${variant}`], disabled && styles.buttonTextDisabled]}>
        {label}
      </Text>
    </Pressable>
  );
}

export function ChoiceChip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected }}
      hitSlop={4}
      onPress={onPress}
      style={({ pressed }) => [
        styles.chip,
        selected && styles.chipSelected,
        pressed && styles.buttonPressed,
      ]}>
      <Text allowFontScaling style={[styles.chipText, selected && styles.chipTextSelected]}>
        {label}
      </Text>
    </Pressable>
  );
}

export function Notice({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  if (!message) return null;
  return (
    <View accessibilityRole="alert" style={styles.notice}>
      <Text style={styles.noticeText}>{message}</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Cerrar aviso"
        hitSlop={10}
        onPress={onDismiss}
        style={styles.noticeDismiss}>
        <Text style={styles.noticeDismissText}>×</Text>
      </Pressable>
    </View>
  );
}

export function SectionHeading({ title, detail }: { title: string; detail?: string }) {
  return (
    <View style={styles.sectionHeading}>
      <Text accessibilityRole="header" style={styles.sectionTitle}>
        {title}
      </Text>
      {detail ? <Text style={styles.sectionDetail}>{detail}</Text> : null}
    </View>
  );
}

type GarmentPhotoProps = {
  garment: Pick<Garment, 'name' | 'category' | 'color' | 'imageUri'>;
  style?: StyleProp<ViewStyle>;
  imageStyle?: StyleProp<ImageStyle>;
};

export function GarmentPhoto(props: GarmentPhotoProps) {
  return <GarmentPhotoContent key={props.garment.imageUri} {...props} />;
}

function GarmentPhotoContent({ garment, style, imageStyle }: GarmentPhotoProps) {
  const [failed, setFailed] = useState(false);
  const swatch = garmentColors.find((option) => option.name === garment.color)?.value ?? colors.brandSoft;

  return (
    <View style={[styles.photoFrame, { backgroundColor: swatch }, style]}>
      {failed ? (
        <View accessible accessibilityLabel={`Imagen no disponible: ${garment.name}`} style={styles.photoFallback}>
          <View style={[styles.fallbackSwatch, { backgroundColor: swatch }]} />
          <Text style={styles.fallbackCategory}>{categoryLabels[garment.category as Category]}</Text>
          <Text style={styles.fallbackName}>{garment.name}</Text>
        </View>
      ) : (
        <Image
          accessible
          accessibilityLabel={`Foto de ${garment.name}`}
          source={{ uri: garment.imageUri }}
          resizeMode="cover"
          onError={() => setFailed(true)}
          style={[styles.photo, imageStyle]}
        />
      )}
    </View>
  );
}

export function Surface({ children, style }: PropsWithChildren<{ style?: StyleProp<ViewStyle> }>) {
  return <View style={[styles.surface, style]}>{children}</View>;
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.canvas },
  scroll: { flex: 1 },
  pageContent: { flexGrow: 1, paddingBottom: spacing.xl },
  content: {
    width: '100%',
    maxWidth: 1040,
    alignSelf: 'center',
    paddingHorizontal: spacing.md,
    paddingTop: spacing.lg,
    gap: spacing.lg,
  },
  pageTitleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.md },
  pageTitleCopy: { flex: 1, gap: spacing.xs },
  pageTitle: { color: colors.ink, fontSize: 30, lineHeight: 36, fontWeight: '700', letterSpacing: -0.6 },
  pageSubtitle: { color: colors.muted, fontSize: 15, lineHeight: 22 },
  button: {
    minHeight: 48,
    minWidth: 48,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: radius.pill,
    borderWidth: 1,
  },
  button_primary: { backgroundColor: colors.brand, borderColor: colors.brand },
  button_secondary: { backgroundColor: colors.surface, borderColor: colors.line },
  button_quiet: { backgroundColor: 'transparent', borderColor: 'transparent' },
  button_danger: { backgroundColor: colors.accentSoft, borderColor: colors.accentSoft },
  buttonText: { fontSize: 15, lineHeight: 20, fontWeight: '700', textAlign: 'center' },
  buttonText_primary: { color: colors.white },
  buttonText_secondary: { color: colors.ink },
  buttonText_quiet: { color: colors.brand },
  buttonText_danger: { color: colors.danger },
  buttonDisabled: { opacity: 0.55 },
  buttonTextDisabled: { color: colors.muted },
  buttonPressed: { opacity: 0.78 },
  chip: {
    minHeight: 42,
    justifyContent: 'center',
    paddingHorizontal: 15,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
  },
  chipSelected: { borderColor: colors.brand, backgroundColor: colors.brandSoft },
  chipText: { color: colors.ink, fontSize: 14, fontWeight: '600' },
  chipTextSelected: { color: colors.brand },
  notice: {
    minHeight: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.medium,
    backgroundColor: colors.brandSoft,
  },
  noticeText: { flex: 1, color: colors.ink, fontSize: 14, lineHeight: 20 },
  noticeDismiss: { minWidth: 32, minHeight: 32, alignItems: 'center', justifyContent: 'center' },
  noticeDismissText: { color: colors.ink, fontSize: 24, lineHeight: 28 },
  sectionHeading: { gap: 4 },
  sectionTitle: { color: colors.ink, fontSize: 21, lineHeight: 27, fontWeight: '700' },
  sectionDetail: { color: colors.muted, fontSize: 14, lineHeight: 20 },
  photoFrame: { overflow: 'hidden', borderRadius: radius.medium, minHeight: 90 },
  photo: { width: '100%', height: '100%' },
  photoFallback: {
    flex: 1,
    minHeight: 120,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 7,
    padding: spacing.md,
    backgroundColor: colors.canvas,
  },
  fallbackSwatch: { width: 36, height: 36, borderRadius: 18, borderWidth: 1, borderColor: colors.line },
  fallbackCategory: { color: colors.muted, fontSize: 12, textAlign: 'center' },
  fallbackName: { color: colors.ink, fontSize: 14, fontWeight: '700', textAlign: 'center' },
  surface: {
    padding: spacing.md,
    borderRadius: radius.large,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.surface,
  },
});
