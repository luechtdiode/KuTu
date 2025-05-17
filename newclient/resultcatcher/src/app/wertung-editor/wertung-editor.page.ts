import { Component, ViewChild, NgZone } from '@angular/core';
import { WertungContainer, Wertung } from '../backend-types';
import { Subscription, defer, of } from 'rxjs';
import { NavController, Platform, ToastController, AlertController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { NgForm } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Keyboard } from '@capacitor/keyboard';
import { Capacitor, Plugins } from '@capacitor/core';
import { turn10ProgrammNames } from '../utils';

@Component({
    selector: 'app-wertung-editor',
    templateUrl: './wertung-editor.page.html',
    styleUrls: ['./wertung-editor.page.scss'],
    standalone: false
})
export class WertungEditorPage {

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              private alertCtrl: AlertController,
              public toastController: ToastController,
              public backendService: BackendService,
              public platform: Platform,
              private zone: NgZone) {
      // If we navigated to this page, we will have an item available as a nav param
      this.durchgang = backendService.durchgang;
      this.step = backendService.step;
      this.geraetId = backendService.geraet;
      // tslint:disable-next-line:radix
      const itemId = parseInt(this.route.snapshot.paramMap.get('itemId'));
      this.updateUI(backendService.wertungen.find(w => w.id === itemId));
  }
  private itemOriginal: WertungContainer;

  @ViewChild('wertungsform') public form: any;
  @ViewChild('enote') public enote: { setFocus: () => void; };
  @ViewChild('dnote') public dnote: { setFocus: () => void; };

  private subscription: Subscription;

  item: WertungContainer;
  wertung: Wertung;
  nextItem: WertungContainer

  geraetId: number;

  step: number;

  durchgang: string;

  waiting = false;

  isDNoteUsed = true;

  get dNoteLabel() {
    return this.item.isDNoteUsed && turn10ProgrammNames.indexOf(this.item.programm) > -1 ? "A-Note" : "D-Note";
  }

  get eNoteLabel() {
    return this.item.isDNoteUsed && turn10ProgrammNames.indexOf(this.item.programm) > -1 ? "B-Note" : "E-Note";
  }

  ionViewWillLeave() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = undefined;
    }
  }

  ionViewWillEnter() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = undefined;
    }
    this.subscription = this.backendService.wertungUpdated.subscribe(wc => {
      console.log('incoming wertung from service', wc);
      if (wc.wertung.athletId === this.wertung.athletId
         && wc.wertung.wettkampfdisziplinId === this.wertung.wettkampfdisziplinId
         && wc.wertung.endnote !== this.wertung.endnote) {
        console.log('updateing wertung from service');
        this.item.wertung = Object.assign({}, wc.wertung);
        this.itemOriginal.wertung = Object.assign({}, wc.wertung);
        this.wertung = Object.assign({
          noteD: 0.00,
          noteE: 0.00,
          endnote: 0.00
        }, this.item.wertung);
      }
    });
    this.platform.ready().then(() => {

      // We need to use a timeout in order to set the focus on load
      setTimeout(() => {
        let Keyboard;
        if (Capacitor.getPlatform() !== "web") {
          if (Keyboard.show) {
            Keyboard.show(); // Needed for android. Call in a platform ready function
            console.log('keyboard called');
          }
        }
        if (this.isDNoteUsed && this.dnote) {
          this.dnote.setFocus();
          console.log('dnote focused');
        } else if (this.enote) {
          this.enote.setFocus();
          console.log('enote focused');
        }
      }, 400);

    });

  }

  editable() {
    return this.backendService.loggedIn && this.wertung.wettkampfdisziplinId > 0;
  }

  updateUI(wc: WertungContainer) {
    this.zone.run(() => {
      this.waiting = false;
      this.item = Object.assign({}, wc);
      this.isDNoteUsed = this.item.isDNoteUsed;
      this.itemOriginal = Object.assign({}, wc);
      this.wertung = Object.assign({
        noteD: 0.00,
        noteE: 0.00,
        endnote: 0.00
      }, this.itemOriginal.wertung);

      const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === wc.wertung.id);
      let nextItemIndex = currentItemIndex + 1;
      if (currentItemIndex < 0 || currentItemIndex >= this.backendService.wertungen.length - 1) {
        this.nextItem = undefined;
      } else {
        this.nextItem = this.backendService.wertungen[nextItemIndex];
      }

      this.ionViewWillEnter();
    });
  }

  ensureInitialValues(wertung: Wertung): Wertung {
    return Object.assign(this.wertung, wertung);
  }

  saveClose(form: NgForm) {
    if(!form.valid) return;
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe({
      next: (wc) => {
        this.updateUI(wc);
        this.navCtrl.pop();
      },
      error: (err) => {
        this.waiting = false;
        console.log(err);
    }});
  }

  save(form: NgForm) {
    if(!form.valid) return;
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe({
      next: (wc) => {
        this.updateUI(wc);
      },
      error: (err) => {
        this.waiting = false;
        console.log(err);
    }});
  }

  saveNext(form: NgForm) {
    if(!form.valid) return;
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe({
      next: (wc) => {
        this.waiting = false;
        const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === wc.wertung.id);
        if (currentItemIndex < 0) {
          console.log('unexpected wertung - id matches not with current wertung: ' + wc.wertung.id);
        }
        let nextItemIndex = currentItemIndex + 1;
        if (currentItemIndex < 0) {
          nextItemIndex = 0;
        } else if (currentItemIndex >= this.backendService.wertungen.length - 1) {
          if (this.backendService.wertungen.filter(w => w.wertung.endnote === undefined).length === 0) {
            this.navCtrl.pop();
            this.toastSuggestCompletnessCheck();
            return;
          } else {
            nextItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.endnote === undefined);
            this.toastMissingResult(form, this.backendService.wertungen[nextItemIndex].vorname
              + ' ' + this.backendService.wertungen[nextItemIndex].name);
          }
        }
        form.resetForm();
        this.updateUI(this.backendService.wertungen[nextItemIndex]);
      },
      error: (err) => {
        this.waiting = false;
        console.log(err);
    }});
  }

  nextEmptyOrFinish(form: NgForm) {
    const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === this.wertung.id);
    if (this.backendService.wertungen.filter((w, index) => {
      return index > currentItemIndex && w.wertung.endnote === undefined;
    }).length === 0) {
      this.navCtrl.pop();
      this.toastSuggestCompletnessCheck();
      return;
    } else {
      const nextItemIndex = this.backendService.wertungen.findIndex((w, index) => {
        return index > currentItemIndex && w.wertung.endnote === undefined;
      });
      this.toastMissingResult(form, this.backendService.wertungen[nextItemIndex].vorname
        + ' ' + this.backendService.wertungen[nextItemIndex].name);
      form.resetForm();
      this.updateUI(this.backendService.wertungen[nextItemIndex]);
    }
  }

  geraetName(): string {
    return this.backendService.geraete ? this.backendService.geraete.find(g => g.id === this.geraetId).name : '';
  }

  async toastSuggestCompletnessCheck() {
    const toast = await this.toastController.create({
      header: 'Qualitätskontrolle',
      message: 'Es sind jetzt alle Resultate erfasst. Bitte ZU ZWEIT die Resultate prüfen und abschliessen!',
      animated: true,
      position: 'middle',
      buttons: [
        {
          text: 'OK, gelesen.',
          icon: 'checkmark-circle',
          role: 'cancel',
          handler: () => {
            console.log('OK, gelesen. clicked');
          }
        }
      ]
    });
    toast.present();
  }

  async toastMissingResult(form: NgForm, athlet: string) {
    const alert = await this.alertCtrl.create({
      header: 'Achtung',
      subHeader: 'Fehlendes Resultat!',
      message: 'Nach der Erfassung der letzten Wertung in dieser Riege scheint es noch leere Wertungen zu geben. '
                + 'Bitte prüfen, ob ' + athlet.toUpperCase() + ' geturnt hat.',
      buttons:  [
        {
          text: 'Nicht geturnt',
          role: 'edit',
          handler: () => {
            this.nextEmptyOrFinish(form);
          }
        },
        {
          text: 'Korrigieren',
          role: 'cancel',
          handler: () => {
            console.log('Korrigieren clicked');
          }
        }
      ]
    });
    alert.present();
  }
}
