import { Component, ViewChild } from '@angular/core';
import { NavController, NavParams, Keyboard, Platform } from 'ionic-angular';
import { WertungContainer, Wertung } from '../../app/backend-types';
import { BackendService } from '../../app/backend.service';
import { NgForm } from '@angular/forms';

/**
 * Generated class for the WertungEditorPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@Component({
  selector: 'page-wertung-editor',
  templateUrl: 'wertung-editor.html',
})
export class WertungEditorPage {
  private itemOriginal: WertungContainer;

  @ViewChild("wertungsform") public form;
  @ViewChild("enote") public enote;
  @ViewChild("dnote") public dnote;

  constructor(public navCtrl: NavController, public navParams: NavParams, public backendService: BackendService,
    public keyboard: Keyboard, public platform: Platform) {
      // If we navigated to this page, we will have an item available as a nav param
      this.durchgang = navParams.get('durchgang');
      this.step = navParams.get('step');
      this.geraetId = navParams.get('geraetId');
      
      this.itemOriginal = navParams.get('item');
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.item.wertung);
      this.isDNoteUsed = this.item.isDNoteUsed;

      backendService.wertungUpdated.subscribe(wc => {
        if (wc.wertung.id === this.wertung.id && wc.wertung.endnote !== this.wertung.endnote) {
          console.log("updateing wertung from service");
          this.item.wertung = Object.assign({}, wc.wertung);
          this.itemOriginal.wertung = Object.assign({}, wc.wertung);
          this.wertung = Object.assign({}, this.itemOriginal.wertung);
          this.form.form.markAsUnTouched();
        }
      });
  }

  ionViewWillEnter() {

    console.log("ionViewWillEnter");
    this.platform.ready().then(() => {

      // We need to use a timeout in order to set the focus on load
      setTimeout(() => {
        if ((this.keyboard as any).show) {
          (this.keyboard as any).show(); // Needed for android. Call in a platform ready function
          console.log("keyboard called");
        }
        if (this.isDNoteUsed && this.dnote) {
          this.dnote.setFocus();
          console.log("dnote focused");
        } else if (this.enote) {
          this.enote.setFocus();
          console.log("enote focused");
        }        
      },400);

    });

  }

  item: WertungContainer;
  wertung: Wertung;

  geraetId: number;

  step: number;

  durchgang: string;

  waiting = false;

  isDNoteUsed = true;

  editable() {
    return this.backendService.loggedIn
  }
  
  updateUI(wc: WertungContainer) {    
    this.waiting = false;
    this.item = Object.assign({}, wc);
    this.itemOriginal = Object.assign({}, wc);
    this.wertung = Object.assign({}, this.itemOriginal.wertung);
    
    this.ionViewWillEnter();
  }
  
  ensureInitialValues(wertung: Wertung): Wertung {
    if (!wertung.noteD) {
      wertung.noteD = 0;
    }
    if (!wertung.noteE) {
      wertung.noteE = 0;
    }
    return wertung;
  }

  saveClose(wertung: Wertung) {
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(wertung)).subscribe((wc) => {
      this.updateUI(wc);
      this.navCtrl.pop();
    }, (err) => {
      this.waiting = false;
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
      console.log(err);      
    });
  }
  
  save(wertung: Wertung) {
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(wertung)).subscribe((wc) => {
        this.updateUI(wc);
    }, (err) => {
      this.waiting = false;
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
      console.log(err);      
    });
  }

  saveNext(wertung: Wertung) {
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(wertung)).subscribe((wc) => {
      this.waiting = false;
      const currentItemIndex = this.backendService.wertungen.findIndex(wc => wc.wertung.id === wertung.id);
      let nextItemIndex = currentItemIndex + 1;
      if (currentItemIndex < 0) {
        nextItemIndex = 0;
      } else if (currentItemIndex >= this.backendService.wertungen.length-1) {
        this.saveClose(wertung);
        return;
      }
      this.updateUI(this.backendService.wertungen[nextItemIndex]);
    }, (err) => {
      this.waiting = false;
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
      console.log(err);      
    });
  }

  geraetName(): string {
    return this.backendService.geraete ? this.backendService.geraete.find(g => g.id === this.geraetId).name : '';
  }

}
