import { Component, Input } from '@angular/core';
import { NavController } from 'ionic-angular';
import { WertungContainer } from '../../app/backend-types';
import { WertungEditorPage } from '../wertung-editor/wertung-editor';

@Component({
  selector: 'riege-list',
  templateUrl: 'riege-list.html'
})
export class RiegeListPage {
  
  constructor(private navCtrl: NavController) {}
  
  @Input()
  items: WertungContainer[];

  @Input()
  geraetId: number;
  
  @Input()
  durchgang: string;
  
  @Input()
  step: number;

  itemTapped(event, item) {
    // That's right, we're pushing to ourselves!
    this.navCtrl.push(WertungEditorPage, {
      item: item,
      durchgang: this.durchgang,
      step: this.step,
      geraetId: this.geraetId
    });
  }
}
