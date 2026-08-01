import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AlertController, ModalController, ToastController, NavController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { ScoreCalcTemplate, ScoreCalcOptions } from '../backend-types';
import { firstValueFrom } from 'rxjs';
import { ScorecalcEditorModalComponent } from './scorecalc-editor-modal.component';
import { formatDisplayDate } from '../utils';

@Component({
  templateUrl: 'scorecalc.page.html',
  styleUrls: ['scorecalc.page.scss'],
  standalone: false
})
export class ScoreCalcPage {
  uuid = '';
  secret = '';
  titel = '';
  datum = '';
  logoUrl: string | null = null;
  templates: ScoreCalcTemplate[] = [];
  options: ScoreCalcOptions = { disziplinen: [], wettkampfdisziplinen: [] };
  filterText = '';
  loading = false;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private modalCtrl = inject(ModalController);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private nav = inject(NavController);

  ngOnDestroy() {
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
  }

  ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (!stored) {
      this.nav.navigateRoot('/admin/competitions');
      return;
    }
    this.secret = stored.secret;
    this.titel = stored.titel;
    this.datum = stored.datum;
    this.loading = true;
    this.loadData();
    this.loadLogo();
  }

  formatDate(d: string): string {
    return formatDisplayDate(d);
  }

  private loadLogo() {
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
    this.logoUrl = null;
    this.backend.getCompetitionLogo(this.uuid, this.secret).subscribe({
      next: blob => {
        this.logoUrl = URL.createObjectURL(blob);
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  private async loadData() {
    try {
      const [options, templates] = await Promise.all([
        firstValueFrom(this.backend.getScoreCalcOptions(this.uuid, this.secret)),
        firstValueFrom(this.backend.getScoreCalcTemplates(this.uuid, this.secret))
      ]);
      this.options = {
        disziplinen: Array.isArray(options?.disziplinen) ? options.disziplinen : [],
        wettkampfdisziplinen: Array.isArray(options?.wettkampfdisziplinen) ? options.wettkampfdisziplinen : []
      };
      this.templates = Array.isArray(templates) ? templates : [];
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Fehler beim Laden: ' + ((e as any).error || (e as any).message),
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  disziplinName(t: ScoreCalcTemplate): string {
    if (t.disziplinId == null) return '';
    return this.options.disziplinen.find(d => d.id === t.disziplinId)?.name ?? '';
  }

  kategoriedisziplinName(t: ScoreCalcTemplate): string {
    if (t.wettkampfdisziplinId == null) return '';
    return this.options.wettkampfdisziplinen.find(w => w.id === t.wettkampfdisziplinId)?.easyprint ?? '';
  }

  sortOrderOf(t: ScoreCalcTemplate): string {
    const wkm = t.wettkampfId != null ? 100 : 1000;
    const dm = t.disziplinId != null ? 10 : 2000;
    const wdm = t.wettkampfdisziplinId != null ? 1 : 3000;
    return (wkm + dm + wdm).toString().padStart(4, '0');
  }

  isReadOnly(t: ScoreCalcTemplate): boolean {
    return t.wettkampfId == null;
  }

  get filteredTemplates(): ScoreCalcTemplate[] {
    const searchQuery = this.filterText.toUpperCase().split(' ').filter(s => s.length > 0);
    if (searchQuery.length === 0) return this.templates;
    return this.templates.filter(t => {
      return searchQuery.every(search =>
        t.dFormula.toUpperCase().includes(search) ||
        t.eFormula.toUpperCase().includes(search) ||
        t.pFormula.toUpperCase().includes(search) ||
        this.kategoriedisziplinName(t).toUpperCase().includes(search) ||
        this.disziplinName(t).toUpperCase().includes(search)
      );
    });
  }

  fmtAggregate(fn: string | null): string {
    switch (fn) {
      case 'Min': return 'Minimum';
      case 'Max': return 'Maximum';
      case 'Avg': return 'Durchschnitt';
      case 'Sum': return 'Summe';
      default: return '—';
    }
  }

  async newEditor() {
    await this.openModal(null);
  }

  async editEditor(t: ScoreCalcTemplate) {
    if (t.wettkampfId == null) return;
    await this.openModal(t);
  }

  private async openModal(template: ScoreCalcTemplate | null) {
    const modal = await this.modalCtrl.create({
      component: ScorecalcEditorModalComponent,
      componentProps: {
        template,
        uuid: this.uuid,
        secret: this.secret,
        options: this.options
      }
    });
    modal.onDidDismiss().then(async (result) => {
      if (!result.data) return;
      try {
        if (template == null) {
          await firstValueFrom(this.backend.createScoreCalcTemplate(this.uuid, result.data, this.secret));
        } else {
          await firstValueFrom(this.backend.updateScoreCalcTemplate(this.uuid, template.id, result.data, this.secret));
        }
        const toast = await this.toastCtrl.create({ message: 'Formular gespeichert', duration: 2000, color: 'success' });
        await toast.present();
        this.loadData();
      } catch (e) {
        const toast = await this.toastCtrl.create({
          message: 'Fehler beim Speichern: ' + ((e as any).error || (e as any).message),
          duration: 3000,
          color: 'danger'
        });
        await toast.present();
      }
    });
    await modal.present();
  }

  async confirmDelete(t: ScoreCalcTemplate) {
    if (t.wettkampfId == null) return;
    const title = this.kategoriedisziplinName(t) || this.disziplinName(t) || 'Allgemein';
    const alert = await this.alertCtrl.create({
      header: 'Formular löschen',
      message: `Soll das Formular für "${title}" wirklich gelöscht werden?`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Löschen',
          role: 'destructive',
          handler: async () => {
            try {
              await firstValueFrom(this.backend.deleteScoreCalcTemplate(this.uuid, t.id, this.secret));
              const toast = await this.toastCtrl.create({ message: 'Formular gelöscht', duration: 2000, color: 'success' });
              await toast.present();
              this.loadData();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Löschen', duration: 3000, color: 'danger' });
              await toast.present();
            }
          }
        }
      ]
    });
    await alert.present();
  }
}
