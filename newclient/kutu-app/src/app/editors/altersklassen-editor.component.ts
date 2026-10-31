import { Component, Input, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { ItemReorderEventDetail } from '@ionic/core';
import { firstValueFrom } from 'rxjs';
import { AdminBackendService } from '../services/admin-backend.service';

interface KlassenToken {
  bezeichnung: string;
  qualifiers: string[];
  alterVon: number;
  alterBis: number | null;
  schrittweite: number | null;
}

@Component({
  templateUrl: 'altersklassen-editor.component.html',
  standalone: false
})
export class AltersklassenEditorComponent implements OnInit, OnDestroy {
  @Input() initialFormula = '';
  @Input() title = 'Altersklassen bearbeiten';

  rows: KlassenToken[] = [];
  parseError: string | null = null;
  dirty = false;
  editIndex: number | null = null;
  editTokenData: KlassenToken = this.emptyToken();
  editQualifier = '';

  previewValid = signal(false);
  previewLabels = signal<string[]>([]);
  previewError = signal<string | null>(null);
  editPreviewValid = signal(false);
  editPreviewLabels = signal<string[]>([]);
  editPreviewError = signal<string | null>(null);

  private mainPreviewTimer: any = null;
  private editPreviewTimer: any = null;
  private mainPreviewSeq = 0;
  private editPreviewSeq = 0;

  private modalCtrl = inject(ModalController);
  private backend = inject(AdminBackendService);

  ngOnInit() {
    this.parseTokens(this.initialFormula);
    this.requestMainPreview();
  }

  ngOnDestroy() {
    if (this.mainPreviewTimer) clearTimeout(this.mainPreviewTimer);
    if (this.editPreviewTimer) clearTimeout(this.editPreviewTimer);
  }

  private emptyToken(): KlassenToken {
    return { bezeichnung: '', qualifiers: [], alterVon: 7, alterBis: null, schrittweite: null };
  }

  get currentFormula(): string {
    return this.rows.map(t => this.tokenToFormula(t)).filter(Boolean).join(',');
  }

  get valid(): boolean {
    if (this.parseError && !this.dirty) return false;
    return this.previewValid() && !this.previewError();
  }

  get statusText(): string {
    if (this.parseError && !this.dirty) {
      return this.parseError + '. Bitte Formel direkt neu erfassen.';
    }
    if (this.previewError()) return this.previewError()!;
    if (!this.previewValid()) return 'Ungültige Altersklassendefinition.';
    return this.previewLabels().length === 0 ? 'Keine Altersklassen' : 'Formel OK';
  }

  get editTokenCanSave(): boolean {
    return this.editTokenValid && this.editPreviewValid();
  }

  get editFormula(): string {
    if (!this.editTokenValid) return '';
    return this.tokenToFormula(this.editTokenWithQualifiers());
  }

  get editStatusText(): string {
    if (this.editPreviewError()) return this.editPreviewError()!;
    if (!this.editTokenValid) return 'Bitte Bezeichnung und Altersangaben prüfen.';
    if (!this.editPreviewValid()) return 'Ungültige Altersklassendefinition.';
    return this.editPreviewLabels().length === 0 ? 'Keine Altersklassen' : 'Formel OK';
  }

  get editTokenValid(): boolean {
    const t = this.editTokenData;
    if (!t.bezeichnung.trim()) return false;
    if (t.alterVon <= 0) return false;
    if (t.alterBis !== null && t.alterBis < t.alterVon) return false;
    if (t.alterBis === null && t.schrittweite !== null) return false;
    return true;
  }

  rowSummary(t: KlassenToken): string {
    return this.tokenToFormula(t);
  }

  addToken() {
    this.editPreviewSeq++;
    this.editTokenData = this.emptyToken();
    this.editQualifier = '';
    this.editIndex = this.rows.length;
    this.editPreviewValid.set(false);
    this.editPreviewLabels.set([]);
    this.editPreviewError.set(null);
  }

  editToken(index: number) {
    const t = this.rows[index];
    this.editTokenData = { ...t };
    this.editQualifier = t.qualifiers.join('+');
    this.editIndex = index;
    this.editPreviewValid.set(false);
    this.editPreviewLabels.set([]);
    this.editPreviewError.set(null);
    this.requestEditPreview();
  }

  cancelEdit() {
    this.editPreviewSeq++;
    this.editIndex = null;
    this.editPreviewValid.set(false);
    this.editPreviewLabels.set([]);
    this.editPreviewError.set(null);
  }

  saveEdit() {
    const t = { ...this.editTokenData, qualifiers: this.parseQualifiers(this.editQualifier) };
    if (this.editIndex !== null && this.editIndex < this.rows.length) {
      this.rows = this.rows.map((r, i) => i === this.editIndex ? t : r);
    } else {
      this.rows = [...this.rows, t];
    }
    this.dirty = true;
    this.editIndex = null;
    this.scheduleMainPreview();
  }

  deleteToken(index: number) {
    this.dirty = true;
    this.rows = this.rows.filter((_, i) => i !== index);
    this.scheduleMainPreview();
  }

  handleReorder(event: CustomEvent<ItemReorderEventDetail>) {
    const from = event.detail.from;
    const to = event.detail.to;
    const newRows = [...this.rows];
    const [moved] = newRows.splice(from, 1);
    newRows.splice(to, 0, moved);
    this.rows = newRows;
    this.dirty = true;
    event.detail.complete();
    this.scheduleMainPreview();
  }

  onEditChanged() {
    this.editPreviewValid.set(false);
    this.editPreviewLabels.set([]);
    this.editPreviewError.set(null);
    this.scheduleEditPreview();
  }

  addEditEndalter() {
    this.editTokenData.alterBis = 0;
    this.onEditChanged();
  }

  addEditSchrittweite() {
    this.editTokenData.schrittweite = 1;
    this.onEditChanged();
  }

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  confirm() {
    this.modalCtrl.dismiss(this.currentFormula);
  }

  private tokenToFormula(t: KlassenToken): string {
    const qualifierPart = t.qualifiers.length > 0 ? '(' + t.qualifiers.join('+') + ')' : '';
    const prefix = t.bezeichnung.trim() + qualifierPart;
    if (t.alterBis === null || t.alterBis <= 0) return prefix + t.alterVon;
    if (t.schrittweite && t.schrittweite > 1) return prefix + t.alterVon + '-' + t.alterBis + '/' + t.schrittweite;
    return prefix + t.alterVon + '-' + t.alterBis;
  }

  private parseQualifiers(value: string): string[] {
    return (value || '').split(/[+;,]/).map(s => s.trim()).filter(Boolean)
      .filter((v, i, a) => a.indexOf(v) === i);
  }

  private editTokenWithQualifiers(): KlassenToken {
    return { ...this.editTokenData, qualifiers: this.parseQualifiers(this.editQualifier) };
  }

  private scheduleMainPreview() {
    if (this.mainPreviewTimer) clearTimeout(this.mainPreviewTimer);
    this.mainPreviewTimer = setTimeout(() => this.requestMainPreview(), 250);
  }

  private async requestMainPreview() {
    const seq = ++this.mainPreviewSeq;
    if (this.parseError && !this.dirty) {
      this.previewValid.set(false);
      this.previewLabels.set([]);
      this.previewError.set(null);
      return;
    }
    try {
      const response = await firstValueFrom(this.backend.previewAltersklassen(this.currentFormula));
      if (seq === this.mainPreviewSeq) {
        this.previewValid.set(response.valid);
        this.previewLabels.set(response.labels || []);
        this.previewError.set(null);
      }
    } catch (e) {
      if (seq === this.mainPreviewSeq) {
        this.previewValid.set(false);
        this.previewLabels.set([]);
        this.previewError.set('Vorschau fehlgeschlagen: ' + this.errorText(e));
      }
    }
  }

  private scheduleEditPreview() {
    if (this.editPreviewTimer) clearTimeout(this.editPreviewTimer);
    this.editPreviewTimer = setTimeout(() => this.requestEditPreview(), 250);
  }

  private async requestEditPreview() {
    const seq = ++this.editPreviewSeq;
    if (this.editIndex === null || !this.editTokenValid) {
      this.editPreviewValid.set(false);
      this.editPreviewLabels.set([]);
      this.editPreviewError.set(null);
      return;
    }
    try {
      const response = await firstValueFrom(this.backend.previewAltersklassen(this.editFormula));
      if (seq === this.editPreviewSeq) {
        this.editPreviewValid.set(response.valid);
        this.editPreviewLabels.set(response.labels || []);
        this.editPreviewError.set(null);
      }
    } catch (e) {
      if (seq === this.editPreviewSeq) {
        this.editPreviewValid.set(false);
        this.editPreviewLabels.set([]);
        this.editPreviewError.set('Vorschau fehlgeschlagen: ' + this.errorText(e));
      }
    }
  }

  private errorText(e: any): string {
    return e?.error || e?.message || String(e);
  }

  private parseTokens(formula: string) {
    const normalized = (formula || '').trim();
    if (!normalized) { this.rows = []; return; }
    const tokens = normalized.split(',').map(t => t.trim()).filter(Boolean);
    let lastPrefix: { bezeichnung: string; qualifiers: string[] } | null = null;
    const errors: string[] = [];
    const parsed: KlassenToken[] = [];

    for (const token of tokens) {
      const result = this.parseToken(token, lastPrefix);
      if (typeof result === 'string') {
        errors.push(result);
      } else {
        parsed.push(result.token);
        lastPrefix = result.prefix;
      }
    }
    if (errors.length > 0) {
      this.parseError = errors[0];
      this.rows = [];
    } else {
      this.rows = parsed;
    }
  }

  private parseToken(token: string, lastPrefix: { bezeichnung: string; qualifiers: string[] } | null):
    { token: KlassenToken; prefix: { bezeichnung: string; qualifiers: string[] } } | string {
    const rangeStepMatch = token.match(/^(.+?)(?:\(([^)]*)\))?(\d+)-(\d+)\/(\d+)$/);
    const rangeMatch = token.match(/^(.+?)(?:\(([^)]*)\))?(\d+)-(\d+)$/);
    const singleMatch = token.match(/^(.+?)(?:\(([^)]*)\))?(\d+)$/);

    let von: number, bis: number | null = null, schritt: number | null = null;
    let prefixRaw: string, qualifierRaw: string;

    if (rangeStepMatch) {
      prefixRaw = rangeStepMatch[1]; qualifierRaw = rangeStepMatch[2] || '';
      von = parseInt(rangeStepMatch[3], 10);
      bis = parseInt(rangeStepMatch[4], 10);
      schritt = parseInt(rangeStepMatch[5], 10);
    } else if (rangeMatch) {
      prefixRaw = rangeMatch[1]; qualifierRaw = rangeMatch[2] || '';
      von = parseInt(rangeMatch[3], 10);
      bis = parseInt(rangeMatch[4], 10);
    } else if (singleMatch) {
      prefixRaw = singleMatch[1]; qualifierRaw = singleMatch[2] || '';
      von = parseInt(singleMatch[3], 10);
    } else {
      return `Regel kann im Editor nicht dargestellt werden: ${token}`;
    }

    if (bis !== null && bis < von) return `Ungültiger Bereich: ${token}`;
    const prefix = this.parsePrefix(prefixRaw, qualifierRaw, lastPrefix);
    if (typeof prefix === 'string') return prefix;

    return { token: { bezeichnung: prefix.bezeichnung, qualifiers: prefix.qualifiers, alterVon: von, alterBis: bis, schrittweite: schritt }, prefix };
  }

  private parsePrefix(bezeichnungRaw: string, qualifiersRaw: string, lastPrefix: { bezeichnung: string; qualifiers: string[] } | null): { bezeichnung: string; qualifiers: string[] } | string {
    const cleaned = bezeichnungRaw.trim();
    if (!cleaned) {
      if (lastPrefix) return lastPrefix;
      return { bezeichnung: '', qualifiers: [] };
    }
    const qualifiers = this.parseQualifiers(qualifiersRaw);
    return { bezeichnung: cleaned, qualifiers };
  }
}
