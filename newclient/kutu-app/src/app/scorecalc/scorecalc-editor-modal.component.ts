import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import {AlertController, ModalController} from '@ionic/angular';
import { AdminBackendService } from '../services/admin-backend.service';
import {
  ScoreCalcTemplate, ScoreCalcOptions, ScoreCalcVariable,
  ScoreCalcPreviewRequest, ScoreCalcPreviewResponse, WettkampfdisziplinOption
} from '../backend-types';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'scorecalc-editor-modal.component.html',
  styleUrls: ['scorecalc-editor-modal.component.scss'],
  standalone: false
})
export class ScorecalcEditorModalComponent {
  @Input() template: ScoreCalcTemplate | null = null;
  @Input() uuid = '';
  @Input() secret = '';
  @Input() options: ScoreCalcOptions = { disziplinen: [], wettkampfdisziplinen: [] };

  selectedDisziplinId: number | null = null;
  selectedWkdId: number | null = null;
  dFormula = '';
  eFormula = '';
  pFormula = '';
  aggregateFn: string | null = null;

  preview: ScoreCalcPreviewResponse | null = null;
  previewStale = true;
  previewError: string | null = null;
  private valueOverrides = new Map<string, string>();
  private previewSeq = 0;
  private previewTimer: any = null;

  private modalCtrl = inject(ModalController);
  private alertCtrl = inject(AlertController);
  private backend = inject(AdminBackendService);
  private cdr = inject(ChangeDetectorRef);

  ionViewWillEnter() {
    const t = this.template;
    if (t) {
      this.selectedDisziplinId = t.disziplinId;
      this.selectedWkdId = t.wettkampfdisziplinId;
      this.dFormula = t.dFormula;
      this.eFormula = t.eFormula;
      this.pFormula = t.pFormula;
      this.aggregateFn = t.aggregateFn;
    } else {
      const first = this.defaultWkd;
      if (first) {
        this.dFormula = first.isDNoteUsed ? `$${first.dNoteLabel}${first.dNoteLabel} Wert 1.2` : '0';
        this.eFormula = `$${first.eNoteLabel}${first.eNoteLabel} Wert 1.2`;
      }
      this.pFormula = '0';
      this.aggregateFn = null;
    }
    this.applyDNoteVisibility();
    this.valueOverrides.clear();
    this.schedulePreview();
    this.updatePreview();
  }

  ngOnDestroy() {
    if (this.previewTimer) clearTimeout(this.previewTimer);
  }

  get defaultWkd(): WettkampfdisziplinOption | undefined {
    return this.options.wettkampfdisziplinen[0];
  }

  get selectedWkd(): WettkampfdisziplinOption | undefined {
    if (this.selectedWkdId != null) {
      const w = this.options.wettkampfdisziplinen.find(x => x.id === this.selectedWkdId);
      if (w) return w;
    }
    if (this.selectedDisziplinId != null) {
      const w = this.options.wettkampfdisziplinen.find(x => x.disziplinId === this.selectedDisziplinId);
      if (w) return w;
    }
    return this.defaultWkd;
  }

  get isDNoteUsed(): boolean {
    return this.selectedWkd?.isDNoteUsed ?? false;
  }

  get dNoteLabel(): string {
    return this.selectedWkd?.dNoteLabel ?? 'D';
  }

  get eNoteLabel(): string {
    return this.selectedWkd?.eNoteLabel ?? 'E';
  }

  get previewWkdId(): number | null {
    if (this.selectedWkdId != null) return this.selectedWkdId;
    if (this.selectedDisziplinId != null) {
      const w = this.options.wettkampfdisziplinen.find(x => x.disziplinId === this.selectedDisziplinId);
      if (w) return w.id;
    }
    return this.defaultWkd?.id ?? null;
  }

  get committed(): ScoreCalcTemplate {
    return {
      id: this.template?.id ?? 0,
      wettkampfId: this.template?.wettkampfId ?? null,
      disziplinId: this.selectedDisziplinId,
      wettkampfdisziplinId: this.selectedWkdId,
      dFormula: this.dFormula.trim(),
      eFormula: this.eFormula.trim(),
      pFormula: this.pFormula.trim(),
      aggregateFn: this.aggregateFn
    };
  }

  get savingDisabled(): boolean {
    return this.previewStale || !this.preview || !this.preview.valid;
  }

  okState(state: string): boolean {
    return state.includes('OK');
  }

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  confirm() {
    if (this.savingDisabled) return;
    this.confirmUpdate(this.committed);
  }

  async confirmUpdate(t: ScoreCalcTemplate) {
    const alert = await this.alertCtrl.create({
      header: 'Formular speichern - Sicherheitsabfrage',
      message: `Bereits erfasste Wertungen zu den im Formular angegebenen Disziplinen werden beim Speichern der Formulare zurückgesetzt. Soll das Formular wirklich gespeichert werden?`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Speichern',
          role: 'destructive',
          handler: async () => {
            this.modalCtrl.dismiss(this.committed);
          }
        }
      ]
    });
    await alert.present();
  }

  onDisziplinChange() {
    const d = this.options.disziplinen.find(x => x.id === this.selectedDisziplinId);
    const w = this.selectedWkd;
    if (d && w && w.disziplinId !== d.id) {
      this.selectedWkdId = null;
    }
    this.applyDNoteVisibility();
    this.schedulePreview();
  }

  onWkdChange() {
    const w = this.options.wettkampfdisziplinen.find(x => x.id === this.selectedWkdId);
    const d = this.options.disziplinen.find(x => x.id === this.selectedDisziplinId);
    if (d && w && w.disziplinId !== d.id) {
      this.selectedDisziplinId = null;
    }
    this.applyDNoteVisibility();
    this.schedulePreview();
  }

  private applyDNoteVisibility() {
    if (!this.isDNoteUsed) {
      this.dFormula = '0';
    }
  }

  schedulePreview() {
    this.previewStale = true;
    this.previewError = null;
    if (this.previewTimer) clearTimeout(this.previewTimer);
    this.previewTimer = setTimeout(() => this.updatePreview(), 300);
  }

  onValueChange(v: ScoreCalcVariable, value: string | number | null) {
    this.valueOverrides.set(ScorecalcEditorModalComponent.varKey(v), value == null ? '' : String(value));
    this.schedulePreview();
  }

  overrideValue(v: ScoreCalcVariable): string {
    const key = ScorecalcEditorModalComponent.varKey(v);
    const override = this.valueOverrides.get(key);
    if (override !== undefined) return override;
    return v.value == null ? '' : String(v.value);
  }

  private async updatePreview() {
    const wkdId = this.previewWkdId;
    if (wkdId == null) {
      this.previewStale = false;
      this.previewError = 'Keine Kategorie-Disziplin für die Vorschau vorhanden.';
      this.cdr.detectChanges();
      return;
    }
    const seq = ++this.previewSeq;
    const request: ScoreCalcPreviewRequest = {
      wettkampfdisziplinId: wkdId,
      template: this.committed,
      values: this.currentValues()
    };
    try {
      const response = await firstValueFrom(this.backend.previewScoreCalc(this.uuid, request, this.secret));
      if (seq === this.previewSeq) {
        this.preview = response;
        this.valueOverrides.clear();
        this.previewStale = false;
        this.cdr.detectChanges();
      }
    } catch (e) {
      if (seq === this.previewSeq) {
        this.previewStale = false;
        this.previewError = 'Vorschau fehlgeschlagen: ' + ((e as any)?.error || (e as any)?.message || e);
        this.cdr.detectChanges();
      }
    }
  }

  private static varKey(v: { prefix: string; name: string; index: number }): string {
    return `${v.prefix}:${v.name}:${v.index}`;
  }

  private currentValues(): ScoreCalcVariable[] {
    if (!this.preview) return [];
    return this.preview.exercises.reduce<ScoreCalcVariable[]>((acc, ex) =>
      acc.concat(ex.map(v => {
        const key = ScorecalcEditorModalComponent.varKey(v);
        const value = this.valueOverrides.has(key) ? this.valueOverrides.get(key)! : v.value;
        return { ...v, value: isFinite(Number(value)) ? Number(value) : 0 };
      })), []);
  }

  private varCount(formula: string, prefixes: string[]): number {
    const re = /\$([DAEBP])/g;
    let count = 0;
    let m: RegExpExecArray | null;
    while ((m = re.exec(formula)) !== null) {
      if (prefixes.includes(m[1])) count++;
    }
    return count;
  }

  insertDVariable() {
    const n = this.varCount(this.dFormula, ['D', 'A']) + 1;
    this.dFormula += (this.dFormula === '0' || !this.dFormula.endsWith(".2") ? `$${this.dNoteLabel}${this.dNoteLabel} Wert ${n}.2` : ` + $${this.dNoteLabel}${this.dNoteLabel} Wert ${n}.2`);
    this.schedulePreview();
  }

  insertEVariable() {
    const n = this.varCount(this.eFormula, ['E', 'B']) + 1;
    this.eFormula += (this.eFormula === '0' || !this.eFormula.endsWith(".2") ? `$${this.eNoteLabel}${this.eNoteLabel} Wert ${n}.2` : ` + $${this.eNoteLabel}${this.eNoteLabel} Wert ${n}.2`);
    this.schedulePreview();
  }

  insertPVariable() {
    const n = this.varCount(this.pFormula, ['P']) + 1;
    this.pFormula += (this.pFormula === '0' || !this.pFormula.endsWith(".2") ? `$PPenalty ${n}.2` : ` + $PPenalty ${n}.2`);
    this.schedulePreview();
  }
}
