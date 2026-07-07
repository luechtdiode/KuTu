import { Component, Input, inject, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { ItemReorderEventDetail } from '@ionic/core';

interface RotationRule { token: string; }

const REGEL_TYPEN: { label: string; token: string }[] = [
  { label: 'Einfache Rotation (Einfach)', token: 'Einfach' },
  { label: 'Kategorie', token: 'Kategorie' },
  { label: 'Verein', token: 'Verein' },
  { label: 'Geschlecht', token: 'Geschlecht' },
  { label: 'Alter absteigend', token: 'AlterAbsteigend' },
  { label: 'Alter aufsteigend', token: 'AlterAufsteigend' },
  { label: 'Name', token: 'Name' },
  { label: 'Vorname', token: 'Vorname' },
  { label: 'Rotierend', token: 'Rotierend' },
  { label: 'Alternierend invers', token: 'AltInvers' },
];
const ALLOWED_PARTS = new Set(REGEL_TYPEN.map(t => t.token));

@Component({
  templateUrl: 'riegenrotationsregel-editor.component.html',
  standalone: false
})
export class RiegenRotationsregelEditorComponent implements OnInit {
  @Input() initialFormula = '';
  @Input() title = 'Riegenrotationsregel bearbeiten';

  readonly REGEL_TYPEN = REGEL_TYPEN;

  rows: RotationRule[] = [];
  parseError: string | null = null;
  dirty = false;
  editIndex: number | null = null;
  editRuleToken = 'Einfach';

  private modalCtrl = inject(ModalController);

  ngOnInit() {
    this.parseRules(this.initialFormula);
  }

  get currentFormula(): string {
    return this.rows.map(r => r.token).join('/');
  }

  get valid(): boolean {
    return this.validateFormula(this.currentFormula) !== null;
  }

  get statusText(): string {
    if (this.parseError && !this.dirty) return this.parseError + '. Bitte Regel neu erfassen.';
    const result = this.validateFormula(this.currentFormula);
    if (result !== null) return !result ? 'Keine Regel gesetzt' : 'Formel OK';
    return 'Ungültige Regeldefinition.';
  }

  addRule() {
    this.editRuleToken = 'Einfach';
    this.editIndex = this.rows.length;
  }

  editRule(index: number) {
    this.editRuleToken = this.rows[index].token;
    this.editIndex = index;
  }

  saveEdit() {
    const rule: RotationRule = { token: this.editRuleToken };
    if (this.editIndex !== null && this.editIndex < this.rows.length) {
      this.rows = this.rows.map((r, i) => i === this.editIndex ? rule : r);
    } else {
      this.rows = [...this.rows, rule];
    }
    this.dirty = true;
    this.editIndex = null;
  }

  deleteRule(index: number) {
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

  private parseRules(formula: string) {
    const normalized = (formula || '').trim();
    if (!normalized) { this.rows = []; return; }
    const tokens = normalized.split('/').map(t => t.trim()).filter(Boolean);
    const errors: string[] = [];
    const parsed: RotationRule[] = [];
    for (const token of tokens) {
      if (ALLOWED_PARTS.has(token)) {
        parsed.push({ token });
      } else {
        errors.push(`Regel kann im Editor nicht dargestellt werden: ${token}`);
      }
    }
    if (errors.length > 0) {
      this.parseError = errors[0];
      this.rows = [];
    } else {
      this.rows = parsed;
    }
  }

  private validateFormula(value: string): string | null {
    const candidate = (value || '').trim();
    if (!candidate) return '';
    const tokens = candidate.split('/').map(t => t.trim()).filter(Boolean);
    const unknown = tokens.filter(t => !ALLOWED_PARTS.has(t));
    if (unknown.length > 0) return null;
    return candidate;
  }
}
