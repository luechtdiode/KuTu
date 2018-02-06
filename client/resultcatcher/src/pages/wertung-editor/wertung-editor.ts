import { Component, Input } from '@angular/core';
import { NavController, NavParams } from 'ionic-angular';
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

  constructor(public navCtrl: NavController, public navParams: NavParams, public backendService: BackendService) {
      // If we navigated to this page, we will have an item available as a nav param
      this.item = navParams.get('item');
      this.durchgang = navParams.get('durchgang');
      this.step = navParams.get('step');
      this.geraetId = navParams.get('geraetId');
  }

  @Input()
  item: WertungContainer;

  @Input()
  geraetId: number;

  @Input()
  step: number;

  @Input()
  durchgang: string;

  save(wertung: Wertung) {
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, wertung);
  }
  
  geraetName(): string {
    return this.backendService.geraete ? this.backendService.geraete.find(g => g.id === this.geraetId).name : '';
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad WertungEditorPage');
  }

}
