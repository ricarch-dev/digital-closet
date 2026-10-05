import { router, Stack } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { ActionButton } from '@/components/ui';
import { colors } from '@/theme/tokens';

export default function NotFoundScreen() {
  return (
    <>
      <Stack.Screen options={{ title: 'Página no encontrada' }} />
      <View style={styles.container}>
        <Text accessibilityRole="header" style={styles.title}>No encontramos esta pantalla</Text>
        <Text style={styles.description}>Vuelve al clóset para seguir explorando tus prendas.</Text>
        <ActionButton label="Volver al clóset" onPress={() => router.replace('/(tabs)')} />
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
    gap: 14,
    backgroundColor: colors.canvas,
  },
  title: {
    color: colors.ink,
    fontSize: 22,
    lineHeight: 28,
    fontWeight: '700',
    textAlign: 'center',
  },
  description: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 21,
    textAlign: 'center',
  },
});
