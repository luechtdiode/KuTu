import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController, ToastController } from '@ionic/angular';
import { firstValueFrom } from 'rxjs';
import { AdminBackendService } from '../services/admin-backend.service';
import { ApproveEMailRequest, ApproveEMailResponse } from '../backend-types';
import { TermsModalComponent } from '../create-competition/terms-modal.component';

/** Muss mit CreatorMetaData.currentTermsVersion im Backend (package.scala) uebereinstimmen. */
const TERMS_VERSION = '1.0';

@Component({
  templateUrl: 'competition-approval.page.html',
  standalone: false
})
export class CompetitionApprovalPage {
  readonly TERMS_VERSION = TERMS_VERSION;

  readonly uuid = signal<string>(null);
  readonly mail = signal<string>(null);

  readonly creatorName = signal('');
  readonly creatorAddress = signal('');
  readonly creatorPhone = signal('');
  readonly termsAccepted = signal(false);

  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly resultMessage = signal<string>(null);
  readonly resultSuccess = signal(false);

  private backend = inject(AdminBackendService);
  private route = inject(ActivatedRoute);
  private toastCtrl = inject(ToastController);
  private modalCtrl = inject(ModalController);

  constructor() {
    this.uuid.set(this.route.snapshot.paramMap.get('uuid'));
    this.mail.set(this.route.snapshot.queryParamMap.get('mail'));
  }

  isValid(): boolean {
    return !!(this.uuid() && this.mail() &&
      this.creatorName().trim() && this.creatorAddress().trim() && this.creatorPhone().trim() &&
      this.termsAccepted());
  }

  async showTerms(event: Event) {
    event.preventDefault();
    const modal = await this.modalCtrl.create({ component: TermsModalComponent });
    await modal.present();
    const result = await modal.onDidDismiss();
    if (result.data) {
      this.termsAccepted.set(true);
    }
  }

  request(): ApproveEMailRequest {
    return {
      mail: this.mail(),
      creator: {
        creatorName: this.creatorName().trim(),
        creatorAddress: this.creatorAddress().trim(),
        creatorPhone: this.creatorPhone().trim(),
        termsVersion: TERMS_VERSION
      }
    };
  }

  async submit() {
    if (!this.isValid() || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.resultMessage.set(null);
    try {
      const response: ApproveEMailResponse =
        await firstValueFrom(this.backend.approveCompetitionByMail(this.uuid(), this.request()));
      this.resultSuccess.set(!!response.success);
      this.resultMessage.set(response.message);
      this.submitted.set(true);
      if (response.success) {
        this.toast('Wettkampf bestätigt - eine Backup-Email mit dem Admin-Passwort wurde versendet.');
      }
    } catch (error) {
      this.resultSuccess.set(false);
      this.resultMessage.set('Die Bestätigung ist fehlgeschlagen. Bitte den Link aus der Email erneut verwenden.');
    } finally {
      this.submitting.set(false);
    }
  }

  private async toast(message: string) {
    const toast = await this.toastCtrl.create({ message, duration: 6000, color: 'success' });
    await toast.present();
  }
}
