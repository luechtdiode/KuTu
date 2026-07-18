import { Injectable } from '@angular/core';
import { StoredSecret } from '../backend-types';

const SECRETS_KEY = 'kutu-admin-secrets';

@Injectable()
export class SecretService {

  getSecrets(): StoredSecret[] {
    const raw = localStorage.getItem(SECRETS_KEY);
    if (!raw) return [];
    try {
      return JSON.parse(raw);
    } catch {
      return [];
    }
  }

  saveSecret(secret: StoredSecret): void {
    const secrets = this.getSecrets().filter(s => s.uuid !== secret.uuid);
    secrets.push(secret);
    localStorage.setItem(SECRETS_KEY, JSON.stringify(secrets));
  }

  getSecret(uuid: string): StoredSecret | undefined {
    return this.getSecrets().find(s => s.uuid === uuid);
  }

  removeSecret(uuid: string): void {
    const secrets = this.getSecrets().filter(s => s.uuid !== uuid);
    localStorage.setItem(SECRETS_KEY, JSON.stringify(secrets));
  }

  updateStoredSecretTitelDatum(uuid: string, titel: string, datum: string): void {
    const secrets = this.getSecrets().map(s => s.uuid === uuid ? { ...s, titel, datum } : s);
    localStorage.setItem(SECRETS_KEY, JSON.stringify(secrets));
  }
}
