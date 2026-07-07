import { Component, inject, NgZone, ChangeDetectorRef } from '@angular/core';
import { ToastController, NavController, ModalController } from '@ionic/angular';
import { AdminBackendService } from '../services/admin-backend.service';
import { SecretService } from '../services/secret.service';
import { ProgrammRaw } from '../backend-types';
import { formatDateForApi } from '../utils';
import { firstValueFrom } from 'rxjs';
import { AltersklassenEditorComponent } from '../editors/altersklassen-editor.component';
import { RiegenRotationsregelEditorComponent } from '../editors/riegenrotationsregel-editor.component';
import { PunktegleichstandsregelEditorComponent } from '../editors/punktegleichstandsregel-editor.component';
import { TeamregelEditorComponent } from '../editors/teamregel-editor.component';
import { TermsModalComponent } from './terms-modal.component';

const PRESETS_ALTERSKLASSEN = [
  { label: 'Ohne', value: '' },
  { label: 'Turn10®', value: 'AK7-18,AK24,AK30-100/5' },
  { label: 'DTB', value: 'AK6,AK18,AK22,AK25' },
  { label: 'DTB Pflicht', value: 'AK8-9,AK11-19/2' },
  { label: 'DTB Kür', value: 'AK13-19/2' },
  { label: 'Individuell', value: '__custom__' },
];

const PRESETS_PUNKTEGLEICHSTANDSREGEL = [
  { label: 'Ohne - Punktgleichstand => gleicher Rang', value: 'Ohne' },
  { label: 'GeTu Punktgleichstandsregel', value: 'Disziplin(Schaukelringe,Sprung,Reck)' },
  { label: 'KuTu Punktgleichstandsregel', value: 'E-Note-Summe/D-Note-Summe/JugendVorAlter' },
  { label: 'KuTu STV Punktgleichstandsregel', value: 'StreichWertungen(Endnote,Min)/StreichWertungen(E-Note,Min)/StreichWertungen(D-Note,Min)' },
  { label: 'Individuell', value: '__custom__' },
];

const PRESETS_ROTATION = [
  { label: 'Einfache Rotation', value: 'Einfach' },
  { label: 'Verschiebende Rotation', value: 'Einfach/Rotierend' },
  { label: 'Verschiebende Rotation alternierend invers', value: 'Einfach/Rotierend/AltInvers' },
  { label: 'Individuell', value: '__custom__' },
];

const PRESETS_TEAMREGEL = [
  { label: 'Keine Teams', value: '' },
  { label: 'Aus Verein, drei Bestnoten pro Gerät, unbeschränkt', value: 'VereinGerät(3/*)' },
  { label: 'Aus Verein, drei Bestnoten pro Gerät, max vier', value: 'VereinGerät(3/4)' },
  { label: 'Aus Verein, drei Bestnoten pro Gerät, max vier, Kategorien K6+K7+KH+KD', value: 'VereinGerät[K6+K7+KD+KH](3/4)' },
  { label: 'Aus Verein, drei Gesamt-Bestnoten, unbeschränkt', value: 'VereinGesamt(3/*)' },
  { label: 'Aus Verein, drei Gesamt-Bestnoten, max vier', value: 'VereinGesamt(3/4)' },
  { label: 'Aus Verein, Durchschnitt pro Gerät, unbeschränkt (M/W)', value: 'VereinGerät[M+W](avg/*/*)' },
  { label: 'Aus Verein, Median pro Gerät, unbeschränkt (M/W)', value: 'VereinGerät[M+W](median/*/*)' },
  { label: 'Aus Verband, drei Bestnoten pro Gerät, unbeschränkt', value: 'VerbandGerät(3/*)' },
  { label: 'Aus Verband, drei Bestnoten pro Gerät, max vier', value: 'VerbandGerät(3/4)' },
  { label: 'Aus Verband, drei Gesamt-Bestnoten, unbeschränkt', value: 'VerbandGesamt(3/*)' },
  { label: 'Aus Verband, drei Gesamt-Bestnoten, max vier', value: 'VerbandGesamt(3/4)' },
  { label: 'Individuell', value: '__custom__' },
];

interface FormModel {
  datum: string;
  titel: string;
  programmId: number | null;
  notificationEMail: string;
  auszeichnung: number;
  auszeichnungendnote: number;
  altersklassen: string;
  jahrgangsklassen: string;
  punktegleichstandsregel: string;
  rotation: string;
  teamrule: string;
  creatorName: string;
  creatorAddress: string;
  creatorPhone: string;
  termsAccepted: boolean;
}

@Component({
  templateUrl: 'create-competition.page.html',
  standalone: false
})
export class CreateCompetitionPage {
  readonly PRESETS_ALTERSKLASSEN = PRESETS_ALTERSKLASSEN;
  readonly PRESETS_PUNKTEGLEICHSTANDSREGEL = PRESETS_PUNKTEGLEICHSTANDSREGEL;
  readonly PRESETS_ROTATION = PRESETS_ROTATION;
  readonly PRESETS_TEAMREGEL = PRESETS_TEAMREGEL;

  form: FormModel = {
    datum: new Date().toISOString().split('T')[0],
    titel: '',
    programmId: null,
    notificationEMail: '',
    auszeichnung: 40,
    auszeichnungendnote: 0,
    altersklassen: '',
    jahrgangsklassen: '',
    punktegleichstandsregel: '',
    rotation: '',
    teamrule: '',
    creatorName: '',
    creatorAddress: '',
    creatorPhone: '',
    termsAccepted: false
  };

  programme: ProgrammRaw[] = [];
  submitting = false;
  disziplinen: string[] = [];
  kategorien: string[] = [];

  altersklassenPreset = 'Ohne';
  jahrgangsklassenPreset = 'Ohne';
  punktegleichstandsregelPreset = 'Ohne - Punktgleichstand => gleicher Rang';
  rotationPreset = 'Einfache Rotation';
  teamrulePreset = 'Keine Teams';

  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);
  private backend = inject(AdminBackendService);
  private secretService = inject(SecretService);
  private toastCtrl = inject(ToastController);
  private nav = inject(NavController);
  private modalCtrl = inject(ModalController);

  ionViewWillEnter() {
    this.backend.getProgramme().subscribe(pgm => {
      this.programme = pgm;
    });
  }

  onProgramChange() {
    if (!this.form.programmId) {
      this.disziplinen = [];
      this.kategorien = [];
      return;
    }
    this.backend.getDisziplinenForProgram(this.form.programmId).subscribe(d => this.disziplinen = d);
    this.backend.getKategorienForProgram(this.form.programmId).subscribe(k => this.kategorien = k);
  }

  shortLabel(label: string): string {
    if (label.length <= 25) return label;
    return label.substring(0, 22) + '...';
  }

  applyPreset(field: keyof FormModel, presetLabel: string, presets: { label: string; value: string }[]) {
    const preset = presets.find(p => p.label === presetLabel);
    if (!preset) return;
    if (preset.value === '__custom__') return;
    console.log("setting " + preset.value + " on field " + field);
    (this.form as any)[field] = preset.value;
  }

  isValid(): boolean {
    return !!(
      this.form.titel.trim() &&
      this.form.programmId &&
      this.form.notificationEMail.trim() &&
      this.form.creatorName.trim() &&
      this.form.creatorAddress.trim() &&
      this.form.creatorPhone.trim() &&
      this.form.termsAccepted
    );
  }

  async showTerms(event: Event) {
    event.preventDefault();
    const modal = await this.modalCtrl.create({
      component: TermsModalComponent
    });
    await modal.present();
    const { data } = await modal.onDidDismiss();
    if (data === true) {
      this.form.termsAccepted = true;
    }
  }

  async openAltersklassenEditor(field: 'altersklassen' | 'jahrgangsklassen') {
    const modal = await this.modalCtrl.create({
      component: AltersklassenEditorComponent,
      componentProps: {
        initialFormula: this.form[field],
        title: field === 'altersklassen' ? 'Altersklassen bearbeiten' : 'Jahrgangs-Altersklassen bearbeiten'
      }
    });
    modal.onDidDismiss().then(result => {
      this.ngZone.run(() => {
        if (result.data !== null && result.data !== undefined) {
          this.form[field] = result.data;
          this.cdr.detectChanges();
        }
      });
    });
    await modal.present();
  }

  async openPunktegleichstandsregelEditor() {
    const modal = await this.modalCtrl.create({
      component: PunktegleichstandsregelEditorComponent,
      componentProps: {
        initialFormula: this.form.punktegleichstandsregel,
        availableDisziplinen: this.disziplinen
      }
    });
    modal.onDidDismiss().then(result => {
      this.ngZone.run(() => {
        if (result.data !== null && result.data !== undefined) {
          this.form.punktegleichstandsregel = result.data;
          this.cdr.detectChanges();
        }
      });
    });
    await modal.present();
  }

  async openRiegenRotationsregelEditor() {
    const modal = await this.modalCtrl.create({
      component: RiegenRotationsregelEditorComponent,
      componentProps: {
        initialFormula: this.form.rotation
      }
    });
    modal.onDidDismiss().then(result => {
      this.ngZone.run(() => {
        if (result.data !== null && result.data !== undefined) {
          this.form.rotation = result.data;
          this.cdr.detectChanges();
        }
      });
    });
    await modal.present();
  }

  async openTeamregelEditor() {
    const modal = await this.modalCtrl.create({
      component: TeamregelEditorComponent,
      componentProps: {
        initialFormula: this.form.teamrule,
        availableCategories: this.kategorien
      }
    });
    modal.onDidDismiss().then(result => {
      this.ngZone.run(() => {
        if (result.data !== null && result.data !== undefined) {
          this.form.teamrule = result.data;
          this.cdr.detectChanges();
        }
      });
    });
    await modal.present();
  }

  async submit() {
    if (this.submitting) return;
    this.submitting = true;

    try {
      const datumStr = formatDateForApi(new Date(this.form.datum + 'T12:00:00.000Z'));
      const response = await firstValueFrom(
        this.backend.createCompetition({
          datum: datumStr,
          titel: this.form.titel,
          programmId: this.form.programmId!,
          notificationEMail: this.form.notificationEMail,
          auszeichnung: this.form.auszeichnung || 40,
          auszeichnungendnote: this.form.auszeichnungendnote || 0,
          altersklassen: this.form.altersklassen,
          jahrgangsklassen: this.form.jahrgangsklassen,
          punktegleichstandsregel: this.form.punktegleichstandsregel,
          rotation: this.form.rotation,
          teamrule: this.form.teamrule,
          creatorName: this.form.creatorName,
          creatorAddress: this.form.creatorAddress,
          creatorPhone: this.form.creatorPhone,
          termsAccepted: true,
          termsVersion: '1.0'
        })
      );

      this.secretService.saveSecret({
        uuid: response.uuid,
        titel: response.titel,
        datum: response.datum,
        secret: response.secret
      });

      const toast = await this.toastCtrl.create({
        message: `Wettkampf "${response.titel}" angelegt! Prüfe dein Email-Postfach zur Bestätigung.`,
        duration: 5000,
        color: 'success',
        buttons: [
          {
            text: 'Download ZIP',
            handler: () => {
              this.backend.downloadCompetitionZip(response.uuid, response.secret)
                .subscribe(blob => {
                  const a = document.createElement('a');
                  a.href = URL.createObjectURL(blob);
                  a.download = response.titel + '.zip';
                  a.click();
                });
            }
          }
        ]
      });
      await toast.present();
      this.nav.navigateRoot('/admin/competitions');
    } catch (e: any) {
      const toast = await this.toastCtrl.create({
        message: 'Fehler: ' + (e.error || e.message || 'Unbekannter Fehler'),
        duration: 5000,
        color: 'danger'
      });
      await toast.present();
    } finally {
      this.submitting = false;
    }
  }
}
