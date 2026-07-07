import { Component, Input, inject, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { ItemReorderEventDetail } from '@ionic/core';

interface StructuredRule {
  ruleType1: string;
  ruleType2: string;
  aggregate: string;
  min: string;
  max: string;
  includeSexGrouping: boolean;
  selectedCategories: string[];
  extraTeams: string[];
}

const RULE_TYPES_1 = ['Keine Teams', 'Aus Verein', 'Aus Verband'];
const RULE_TYPES_2 = ['Gesamtwertung', 'Gerätewertung'];
const AGGREGATES = ['Sum', 'Avg', 'Med', 'Min', 'Max', 'DevMin', 'DevMax'];

@Component({
  templateUrl: 'teamregel-editor.component.html',
  standalone: false
})
export class TeamregelEditorComponent implements OnInit {
  @Input() initialFormula = '';
  @Input() title = 'Teamregel bearbeiten';
  @Input() availableCategories: string[] = [];

  readonly RULE_TYPES_1 = RULE_TYPES_1;
  readonly RULE_TYPES_2 = RULE_TYPES_2;
  readonly AGGREGATES = AGGREGATES;

  rows: StructuredRule[] = [];
  parseError: string | null = null;
  dirty = false;
  editIndex: number | null = null;
  editRule: StructuredRule = this.emptyRule();
  editSelectedCategoriesSet = new Set<string>();

  private modalCtrl = inject(ModalController);

  ngOnInit() {
    this.parseRules(this.initialFormula);
  }

  private emptyRule(): StructuredRule {
    return { ruleType1: 'Aus Verein', ruleType2: 'Gesamtwertung', aggregate: 'Sum', min: '*', max: '*', includeSexGrouping: false, selectedCategories: [], extraTeams: [] };
  }

  get currentFormula(): string {
    return this.rows.map(r => this.ruleToFormula(r)).filter(Boolean).join(',');
  }

  get valid(): boolean {
    return this.validateFormula(this.currentFormula) !== null;
  }

  get statusText(): string {
    if (this.parseError && !this.dirty) return this.parseError + '. Bitte im Feld direkt bearbeiten oder Regeln neu erfassen.';
    const result = this.validateFormula(this.currentFormula);
    if (result !== null) return !result ? 'Keine Teams' : 'Formel OK';
    return 'Ungültige Teamregel.';
  }

  get editRuleValid(): boolean {
    if (this.editRule.ruleType1 === 'Keine Teams') return true;
    // Validate min/max
    const minOk = this.editRule.min === '*' || (!!this.editRule.min && /^\d+$/.test(this.editRule.min) && parseInt(this.editRule.min) > 0);
    const maxOk = this.editRule.max === '*' || (!!this.editRule.max && /^\d+$/.test(this.editRule.max) && parseInt(this.editRule.max) > 0);
    return minOk && maxOk;
  }

  ruleSummary(r: StructuredRule): string {
    return this.ruleToFormula(r);
  }

  addRule() {
    this.editRule = this.emptyRule();
    this.editSelectedCategoriesSet = new Set();
    this.editIndex = this.rows.length;
  }

  editRuleAt(index: number) {
    const r = this.rows[index];
    this.editRule = { ...r, selectedCategories: [...r.selectedCategories], extraTeams: [...r.extraTeams] };
    this.editSelectedCategoriesSet = new Set(r.selectedCategories);
    this.editIndex = index;
  }

  toggleCategory(cat: string) {
    if (this.editSelectedCategoriesSet.has(cat)) {
      this.editSelectedCategoriesSet.delete(cat);
    } else {
      this.editSelectedCategoriesSet.add(cat);
    }
    this.editRule.selectedCategories = Array.from(this.editSelectedCategoriesSet).sort();
  }

  onExtraTeamsChange(event: any) {
    const val = (event.target as any)?.value ?? '';
    this.editRule.extraTeams = val.split(/[,\n\r;]/).map((s: string) => s.trim()).filter(Boolean).filter((v: string, i: number, a: string[]) => a.indexOf(v) === i);
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
    if (r.ruleType1 === 'Keine Teams') return '';
    const rt1 = r.ruleType1 === 'Aus Verband' ? 'Verband' : 'Verein';
    const rt2 = r.ruleType2 === 'Gesamtwertung' ? 'Gesamt' : 'Gerät';
    const groups: string[] = [];
    if (r.includeSexGrouping) groups.push('M+W');
    if (r.selectedCategories.length > 0) groups.push(r.selectedCategories.join('+'));
    const groupPart = groups.length > 0 ? '[' + groups.join('/') + ']' : '';
    const aggPrefix = r.aggregate !== 'Sum' ? r.aggregate : '';
    const extraTeamsPart = r.extraTeams.length > 0 ? '/' + r.extraTeams.join('+') : '';
    return `${rt1}${rt2}${groupPart}(${aggPrefix}${r.min}/${r.max}${extraTeamsPart})`;
  }

  private parseRules(formula: string) {
    const normalized = (formula || '').trim();
    if (!normalized || normalized === 'Keine Teams') { this.rows = []; return; }
    const tokens = normalized.split(',').map(t => t.trim()).filter(Boolean);
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
    if (token === 'Keine Teams') {
      return this.emptyRule();
    }
    // Pattern: VereinGerät[M+W/K6+K7](Sum3/4/TeamA+TeamB) or similar
    const match = token.match(/^(Verein|Verband)(Gesamt|Gerät)(?:\[([^\]]*)\])?\(([^)]*)\)$/);
    if (!match) return null;

    const rt1 = match[1] === 'Verband' ? 'Aus Verband' : 'Aus Verein';
    const rt2 = match[2] === 'Gesamt' ? 'Gesamtwertung' : 'Gerätewertung';
    const groupStr = match[3] || '';
    const params = match[4];

    // Parse groups: M+W and categories
    let includeSexGrouping = false;
    const selectedCategories: string[] = [];
    if (groupStr) {
      const parts = groupStr.split('/').map(s => s.trim()).filter(Boolean);
      for (const part of parts) {
        if (part === 'M+W') {
          includeSexGrouping = true;
        } else {
          const cats = part.split('+').map(s => s.trim()).filter(Boolean);
          selectedCategories.push(...cats);
        }
      }
    }

    // Parse params: optionally aggregate prefix, then min/max, then extra teams
    // Examples: 3/4, avg3/4, 3/4/TeamA+TeamB, avg3/4/TeamA+TeamB
    const paramMatch = params.match(/^((Sum|Avg|Med|Min|Max|DevMin|DevMax))?(\d+|\*)\/(\d+|\*)(?:\/(.+))?$/i);
    if (!paramMatch) return null;

    const aggregate = paramMatch[2] || 'Sum';
    const min = paramMatch[3];
    const max = paramMatch[4];
    const extraStr = paramMatch[5] || '';
    const extraTeams = extraStr ? extraStr.split('+').map(s => s.trim()).filter(Boolean) : [];

    return { ruleType1: rt1, ruleType2: rt2, aggregate, min, max, includeSexGrouping, selectedCategories, extraTeams };
  }

  private validateFormula(value: string): string | null {
    const candidate = (value || '').trim();
    if (!candidate) return '';
    const tokens = candidate.split(',').map(t => t.trim()).filter(Boolean);
    const unsupported = tokens.filter(t => this.parseRuleToken(t) === null && t !== 'Keine Teams');
    if (unsupported.length > 0) return null;
    return candidate;
  }
}
