import { Component, ViewChild } from '@angular/core';
import { WertungContainer, Wertung } from '../backend-types';
import { Subscription } from 'rxjs';
import { NavController, NavParams, Platform } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { NgForm } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-wertung-editor',
  templateUrl: './wertung-editor.page.html',
  styleUrls: ['./wertung-editor.page.scss'],
})
export class WertungEditorPage /*implements OnInit*/ {

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              public backendService: BackendService,
              /*public keyboard: Keyboard,*/
              public platform: Platform) {
      // If we navigated to this page, we will have an item available as a nav param
      this.durchgang = backendService.durchgang;
      this.step = backendService.step;
      this.geraetId = backendService.geraet;
      // tslint:disable-next-line:radix
      const itemId = parseInt(route.snapshot.paramMap.get('itemId'));
      this.updateUI(backendService.wertungen.find(w => w.id === itemId));
      this.isDNoteUsed = this.item.isDNoteUsed;

  }
  private itemOriginal: WertungContainer;

  @ViewChild('wertungsform') public form: any;
  @ViewChild('enote') public enote: { setFocus: () => void; };
  @ViewChild('dnote') public dnote: { setFocus: () => void; };

  private subscription: Subscription;

  item: WertungContainer;
  wertung: Wertung;

  geraetId: number;

  step: number;

  durchgang: string;

  waiting = false;

  isDNoteUsed = true;

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
      // console.log("incoming wertung from service", wc);
      if (wc.wertung.athletId === this.wertung.athletId
         && wc.wertung.wettkampfdisziplinId === this.wertung.wettkampfdisziplinId
         && wc.wertung.endnote !== this.wertung.endnote) {
        // console.log("updateing wertung from service");
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
        /*if ((this.keyboard as any).show) {
          (this.keyboard as any).show(); // Needed for android. Call in a platform ready function
          console.log('keyboard called');
        }*/
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
    return this.backendService.loggedIn;
  }

  updateUI(wc: WertungContainer) {
    this.waiting = false;
    this.item = Object.assign({}, wc);
    this.itemOriginal = Object.assign({}, wc);
    this.wertung = Object.assign({
      noteD: 0.00,
      noteE: 0.00,
      endnote: 0.00
    }, this.itemOriginal.wertung);

    this.ionViewWillEnter();

  }

  ensureInitialValues(wertung: Wertung): Wertung {
    return Object.assign(this.wertung, wertung);
  }

  saveClose(form: NgForm) {
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe((wc) => {
      this.updateUI(wc);
      this.navCtrl.pop();
    }, (err) => {
      this.updateUI(this.itemOriginal);
      console.log(err);
    });
  }

  save(form: NgForm) {
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe((wc) => {
        this.updateUI(wc);
    }, (err) => {
      this.updateUI(this.itemOriginal);
      console.log(err);
    });
  }

  saveNext(form: NgForm) {
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe((wc) => {
      this.waiting = false;
      const currentItemIndex = this.backendService.wertungen.findIndex(wc => wc.wertung.id === this.wertung.id);
      let nextItemIndex = currentItemIndex + 1;
      if (currentItemIndex < 0) {
        nextItemIndex = 0;
      } else if (currentItemIndex >= this.backendService.wertungen.length - 1) {
        this.backendService.loadWertungen();
        this.navCtrl.pop();
        return;
      }
      form.resetForm();
      this.updateUI(this.backendService.wertungen[nextItemIndex]);
    }, (err) => {
      this.updateUI(this.itemOriginal);
      console.log(err);
    });
  }

  geraetName(): string {
    return this.backendService.geraete ? this.backendService.geraete.find(g => g.id === this.geraetId).name : '';
  }
}
