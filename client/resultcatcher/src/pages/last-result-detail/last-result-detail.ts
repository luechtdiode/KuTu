import { Component } from '@angular/core';
import { NavController, NavParams } from 'ionic-angular';
import { BackendService } from '../../app/backend.service';
import { WertungContainer } from '../../app/backend-types';

/**
 * Generated class for the LastResultDetailPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@Component({
  selector: 'page-last-result-detail',
  templateUrl: 'last-result-detail.html',
})
export class LastResultDetailPage {

  item: WertungContainer;

  constructor(public navCtrl: NavController, public navParams: NavParams, public backendService: BackendService) {
    this.item = navParams.get('item');
  }

  geraetText(geraetId: number): string {
    if (!this.backendService.geraete) return '';
    let candidate = this.backendService.geraete
      .filter(c => c.id === geraetId)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }
}
