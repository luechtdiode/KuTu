import { Component, Input } from '@angular/core';
import { NavController } from '@ionic/angular';
import { WertungContainer } from '../../backend-types';
import { BackendService } from '../../services/backend.service';
@Component({
  selector: 'riege-list',
  templateUrl: 'riege-list.component.html',
  styleUrls: ['./riege-list.component.scss'],
})
export class RiegeListComponent {
  constructor(private navCtrl: NavController, private backendService: BackendService) { }
  @Input()
  items: WertungContainer[];


  itemTapped(event: any, item: WertungContainer) {
    if (this.backendService.loggedIn) {
      this.navCtrl.navigateForward('wertung-editor/' + item.id);
    } else {
      this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${item.id}`);
    }
  }
}
