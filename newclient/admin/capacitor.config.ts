import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'ch.seidel.kutu.admin',
  appName: 'KuTu Admin',
  webDir: 'www',
  server: {
    androidScheme: 'https'
  }
};

export default config;
