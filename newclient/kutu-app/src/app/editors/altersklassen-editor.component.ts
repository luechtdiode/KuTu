import { Component, Input, inject, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { ItemReorderEventDetail } from '@ionic/core';

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
export class AltersklassenEditorComponent implements OnInit {
  @Input() initialFormula = '';
  @Input() title = 'Altersklassen bearbeiten';

  rows: KlassenToken[] = [];
  parseError: string | null = null;
  dirty = false;
  editIndex: number | null = null;
  editTokenData: KlassenToken = this.emptyToken();
  editQualifier = '';

  private modalCtrl = inject(ModalController);

  ngOnInit() {
    this.parseTokens(this.initialFormula);
  }

  private emptyToken(): KlassenToken {
    return { bezeichnung: '', qualifiers: [], alterVon: 7, alterBis: null, schrittweite: null };
  }

  get currentFormula(): string {
    return this.rows.map(t => this.tokenToFormula(t)).filter(Boolean).join(',');
  }

  get valid(): boolean {
    return this.validateFormula(this.currentFormula) !== null;
  }

  get statusText(): string {
    if (this.parseError && !this.dirty) {
      return this.parseError + '. Bitte Formel direkt neu erfassen.';
    }
    const result = this.validateFormula(this.currentFormula);
    if (result !== null) return !result ? 'Keine Altersklassen' : 'Formel OK';
    return this.getValidationError(this.currentFormula);
  }

  private getValidationError(value: string): string {
    return 'Ungültige Altersklassendefinition.';
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
    this.editTokenData = this.emptyToken();
    this.editQualifier = '';
    this.editIndex = this.rows.length;
  }

  editToken(index: number) {
    const t = this.rows[index];
    this.editTokenData = { ...t };
    this.editQualifier = t.qualifiers.join('+');
    this.editIndex = index;
  }

  saveEdit() {
    const t = { ...this.editTokenData };
    t.qualifiers = this.editQualifier.split(/[+;,]/).map(s => s.trim()).filter(Boolean)
      .filter((v, i, a) => a.indexOf(v) === i);
    if (this.editIndex !== null && this.editIndex < this.rows.length) {
      this.rows = this.rows.map((r, i) => i === this.editIndex ? t : r);
    } else {
      this.rows = [...this.rows, t];
    }
    this.dirty = true;
    this.editIndex = null;
  }

  deleteToken(index: number) {
    this.dirty = true;
    this.rows = this.rows.filter((_, i) => i !== index);
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
    const qualifiers = qualifiersRaw.split(/[+;,]/).map(s => s.trim()).filter(Boolean)
      .filter((v, i, a) => a.indexOf(v) === i);
    return { bezeichnung: cleaned, qualifiers };
  }

  private validateFormula(value: string): string | null {
    const candidate = (value || '').trim();
    if (!candidate) return '';
    return candidate;
  }
}
