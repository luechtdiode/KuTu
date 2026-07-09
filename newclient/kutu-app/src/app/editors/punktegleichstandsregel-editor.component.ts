import { Component, Input, inject, OnInit } from '@angular/core';
import { ModalController, ReorderEndCustomEvent } from '@ionic/angular';
import { ItemReorderEventDetail } from '@ionic/core';

interface StructuredRule {
  ruleType: string;
  disziplinen: string[];
  typ: string;
  minMax: string;
}

const RULE_TYPES = ['Ohne', 'E-Note-Summe', 'E-Note-Best', 'D-Note-Summe', 'D-Note-Best', 'JugendVorAlter', 'Disziplin', 'StreichDisziplin', 'StreichWertungen'];
const STREICH_TYPEN = ['Endnote', 'E-Note', 'D-Note'];
const MINMAX_TYPEN = ['Min', 'Max'];

@Component({
  templateUrl: 'punktegleichstandsregel-editor.component.html',
  standalone: false
})
export class PunktegleichstandsregelEditorComponent implements OnInit {
  @Input() initialFormula = '';
  @Input() title = 'Punktegleichstandsregel bearbeiten';
  @Input() availableDisziplinen: string[] = [];

  readonly RULE_TYPES = RULE_TYPES;
  readonly STREICH_TYPEN = STREICH_TYPEN;
  readonly MINMAX_TYPEN = MINMAX_TYPEN;

  rows: StructuredRule[] = [];
  parseError: string | null = null;
  dirty = false;
  editIndex: number | null = null;
  editRule: StructuredRule = this.emptyRule();

  private modalCtrl = inject(ModalController);

  ngOnInit() {
    this.parseRules(this.initialFormula);
  }

  private emptyRule(): StructuredRule {
    return { ruleType: 'E-Note-Summe', disziplinen: [], typ: 'Endnote', minMax: 'Min' };
  }

  get currentFormula(): string {
    return this.rows.map(r => this.ruleToFormula(r)).join('/');
  }

  get valid(): boolean {
    return this.validateFormula(this.currentFormula) !== null;
  }

  get statusText(): string {
    if (this.parseError && !this.dirty) return this.parseError + '. Bitte Regel direkt neu erfassen.';
    const result = this.validateFormula(this.currentFormula);
    if (result !== null) return !result ? 'Keine Regel gesetzt' : 'Formel OK';
    return 'Ungültige Regeldefinition.';
  }

  get editRuleValid(): boolean {
    if (this.editRule.ruleType === 'Disziplin' || this.editRule.ruleType === 'StreichDisziplin') {
      return this.editRule.disziplinen.length > 0;
    }
    return true;
  }

  get remainingDisziplinen(): string[] {
    const selected = new Set(this.editRule.disziplinen);
    return this.availableDisziplinen.filter(d => !selected.has(d));
  }

  ruleSummary(r: StructuredRule): string {
    return this.ruleToFormula(r);
  }

  addRule() {
    this.editRule = this.emptyRule();
    this.editIndex = this.rows.length;
  }

  editRuleAt(index: number) {
    const r = this.rows[index];
    this.editRule = { ...r, disziplinen: [...r.disziplinen] };
    this.editIndex = index;
  }

  onEditRuleTypeChange() {
    if (this.editRule.ruleType !== 'Disziplin' && this.editRule.ruleType !== 'StreichDisziplin') {
      this.editRule.disziplinen = [];
    }
  }

  removeDisziplin(index: number) {
    this.editRule.disziplinen = this.editRule.disziplinen.filter((_, i) => i !== index);
  }

  addDisziplin(d: string) {
    this.editRule.disziplinen = [...this.editRule.disziplinen, d];
  }

  handleDisziplinReorder(event: CustomEvent<ItemReorderEventDetail>) {
    const from = event.detail.from;
    const to = event.detail.to;
    const newList = [...this.editRule.disziplinen];
    const [moved] = newList.splice(from, 1);
    newList.splice(to, 0, moved);
    this.editRule.disziplinen = newList;
    event.detail.complete();
  }

  saveEdit() {
    const r = { ...this.editRule };
    if (this.editIndex !== null && this.editIndex < this.rows.length) {
      this.rows = this.rows.map((row, i) => i === this.editIndex ? r : row);
    } else {
      this.rows = [...this.rows, r];
    }
    this.dirty = true;
    this.editIndex = null;
  }

  deleteRule(index: number) {
    this.dirty = true;
    this.rows = this.rows.filter((_, i) => i !== index);
  }
  handleReorderEnd(event: ReorderEndCustomEvent) {
    // The `from` and `to` properties contain the index of the item
    // when the drag started and ended, respectively
    console.log('Dragged from index', event.detail.from, 'to', event.detail.to);

    // Finish the reorder and position the item in the DOM based on
    // where the gesture ended. This method can also be called directly
    // by the reorder group.
    event.detail.complete();
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

  private ruleToFormula(r: StructuredRule): string {
    switch (r.ruleType) {
      case 'Disziplin': return `Disziplin(${r.disziplinen.join(',')})`;
      case 'StreichDisziplin': return `StreichDisziplin(${r.disziplinen.join(',')})`;
      case 'StreichWertungen': return `StreichWertungen(${r.typ},${r.minMax})`;
      default: return r.ruleType;
    }
  }

  private parseRules(formula: string) {
    const normalized = (formula || '').trim();
    if (!normalized) { this.rows = []; return; }
    const tokens = normalized.split('/').map(t => t.trim()).filter(Boolean);
    const errors: string[] = [];
    const parsed: StructuredRule[] = [];

    for (const token of tokens) {
      const rule = this.parseRuleToken(token);
      if (rule) {
        parsed.push(rule);
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

  private parseRuleToken(token: string): StructuredRule | null {
    const simpleTypes = ['Ohne', 'E-Note-Summe', 'E-Note-Best', 'D-Note-Summe', 'D-Note-Best', 'JugendVorAlter'];
    if (simpleTypes.includes(token)) {
      return { ruleType: token, disziplinen: [], typ: 'Endnote', minMax: 'Min' };
    }
    const disziplinMatch = token.match(/^Disziplin\((.+)\)$/);
    if (disziplinMatch) {
      return { ruleType: 'Disziplin', disziplinen: disziplinMatch[1].split(',').map(s => s.trim()).filter(Boolean), typ: 'Endnote', minMax: 'Min' };
    }
    const streichDiszMatch = token.match(/^StreichDisziplin\((.+)\)$/);
    if (streichDiszMatch) {
      return { ruleType: 'StreichDisziplin', disziplinen: streichDiszMatch[1].split(',').map(s => s.trim()).filter(Boolean), typ: 'Endnote', minMax: 'Min' };
    }
    const streichWertMatch = token.match(/^StreichWertungen\((Endnote|E-Note|D-Note)(,(Min|Max))*\)$/);
    if (streichWertMatch) {
      return { ruleType: 'StreichWertungen', disziplinen: [], typ: streichWertMatch[1], minMax: streichWertMatch[3] || 'Min' };
    }
    if (token === 'StreichWertungen') {
      return { ruleType: 'StreichWertungen', disziplinen: [], typ: 'Endnote', minMax: 'Min' };
    }
    return null;
  }

  private validateFormula(value: string): string | null {
    const candidate = (value || '').trim();
    if (!candidate) return '';
    const tokens = candidate.split('/').map(t => t.trim()).filter(Boolean);
    const unsupported = tokens.filter(t => this.parseRuleToken(t) === null);
    if (unsupported.length > 0) return null;
    return candidate;
  }
}
