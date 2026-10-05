import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

import { ClosetProvider } from '@/closet/ClosetProvider';
import { colors } from '@/theme/tokens';

export default function RootLayout() {
  return (
    <ClosetProvider>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          headerShown: false,
          animation: 'none',
          contentStyle: { backgroundColor: colors.canvas },
        }}>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="add-garment" />
      </Stack>
    </ClosetProvider>
  );
}
