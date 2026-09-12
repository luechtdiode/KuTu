import {Component, Input, inject, signal} from '@angular/core';
import { ModalController } from '@ionic/angular';
import { AdminBackendService } from '../services/admin-backend.service';

@Component({
  templateUrl: 'admin-access-link-modal.component.html',
  standalone: false
})
export class AdminAccessLinkModalComponent {
  private lastReceived  = 7;
  private readonly linkSignal = signal('');
  @Input()
  set link(v: string) { this.linkSignal.set(v); }
  get link(): string { return this.linkSignal(); }

  private readonly qrUrlSignal = signal('');
  @Input()
  set qrUrl(v: string) { this.qrUrlSignal.set(v); }
  get qrUrl(): string { return this.qrUrlSignal(); }

  set days(v: number) { this._days = v; this.refresh(); }
  get days(): number { return this._days; }

  @Input() uuid = '';
  @Input() secret = '';

  daysOptions = [1, 2, 3, 5, 7, 30, 365, 0];
  private _days = 7;
  isRefreshing = signal(false);

  private modalCtrl = inject(ModalController);
  private backend = inject(AdminBackendService);

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  copyLink() {
    navigator.clipboard.writeText(this.link);
  }

  isUnchanged(): boolean {
    return this.days === this.lastReceived;
  }

  refresh() {
    if (this.isRefreshing() || !this.uuid || !this.secret) return;
    this.isRefreshing.set(true);
    this.backend.createAdminAccessLink(this.uuid, this.secret, this.days).subscribe({
      next: res => {
        this.lastReceived = this.days;
        this.linkSignal.set(res.link);
        this.qrUrlSignal.set(res.qrImage);
        this.isRefreshing.set(false);
      },
      error: () => {
        this.isRefreshing.set(false);
      }
    });
  }
}
