import { Component, inject, input } from '@angular/core';
import { NavController } from '@ionic/angular';
import { WertungContainer } from '../../backend-types';
import { BackendService } from '../../services/backend.service';
@Component({
    selector: 'riege-list',
    templateUrl: 'riege-list.component.html',
    styleUrls: ['./riege-list.component.scss'],
    standalone: false
})
export class RiegeListComponent {
  private navCtrl = inject(NavController);
  private backendService = inject(BackendService);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() { }
  readonly items = input<WertungContainer[]>(undefined);


  itemTapped(event: any, item: WertungContainer) {
    if (this.backendService.loggedIn) {
      this.navCtrl.navigateForward('wertung-editor/' + item.id);
    } else {
      this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${item.id}`);
    }
  }
}
