import { Component, Input } from '@angular/core';
import { NavController } from '@ionic/angular';
import { WertungContainer } from '../../backend-types';
@Component({
  selector: 'riege-list',
  templateUrl: 'riege-list.component.html',
  styleUrls: ['./riege-list.component.scss'],
})
export class RiegeListComponent {
  constructor(private navCtrl: NavController) { }
  @Input()
  items: WertungContainer[];
  itemTapped(event: any, item: WertungContainer) {
    this.navCtrl.navigateForward('wertung-editor/' + item.id);
  }
}
