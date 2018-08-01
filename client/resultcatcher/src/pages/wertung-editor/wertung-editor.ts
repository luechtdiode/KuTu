import { Component } from '@angular/core';
import { NavController, NavParams, NavOptions } from 'ionic-angular';
import { WertungContainer, Wertung } from '../../app/backend-types';
import { BackendService } from '../../app/backend.service';

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
  private items: WertungContainer[];

  constructor(public navCtrl: NavController, public navParams: NavParams, public backendService: BackendService) {
      // If we navigated to this page, we will have an item available as a nav param
      this.durchgang = navParams.get('durchgang');
      this.step = navParams.get('step');
      this.geraetId = navParams.get('geraetId');
      
      this.itemOriginal = navParams.get('item');
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.item.wertung);

      backendService.wertungUpdated.subscribe(wc => {
        if (wc.wertung.id === this.wertung.id) {
          this.item.wertung = Object.assign({}, wc.wertung);
          this.itemOriginal.wertung = Object.assign({}, wc.wertung);
          this.wertung = Object.assign({}, this.itemOriginal.wertung);
        }
      });
  }

  item: WertungContainer;
  wertung: Wertung;

  geraetId: number;

  step: number;

  durchgang: string;

  editable() {
    return this.backendService.loggedIn
  }

  save(wertung: Wertung) {
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, wertung).subscribe((wc) => {
      this.item = Object.assign({}, wc);
      this.itemOriginal = Object.assign({}, wc);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
      this.navCtrl.pop();
    }, (err) => {
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
      console.log(err);      
    });
  }
  
  saveNext(wertung: Wertung) {
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, wertung).subscribe((wc) => {
      const currentItemIndex = this.backendService.wertungen.findIndex(wc => wc.wertung.id === wertung.id);
      let nextItemIndex = currentItemIndex + 1;
      if (currentItemIndex < 0) {
        nextItemIndex = 0;
      } else if (currentItemIndex >= this.backendService.wertungen.length-1) {
        this.save(wertung);
        return;
      }
      const wertungsContainer: WertungContainer = this.backendService.wertungen[nextItemIndex];
      this.item = Object.assign({}, wertungsContainer);
      this.itemOriginal = Object.assign({}, wertungsContainer);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
    }, (err) => {
      this.item = Object.assign({}, this.itemOriginal);
      this.wertung = Object.assign({}, this.itemOriginal.wertung);
      console.log(err);      
    });
  }

  geraetName(): string {
    return this.backendService.geraete ? this.backendService.geraete.find(g => g.id === this.geraetId).name : '';
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad WertungEditorPage');
  }

}
