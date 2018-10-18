import { Component, Input } from '@angular/core';
import { WertungContainer } from '../../app/backend-types';

/**
 * Generated class for the ResultDisplayComponent component.
 *
 * See https://angular.io/api/core/Component for more info on Angular
 * Components.
 */
@Component({
  selector: 'result-display',
  templateUrl: 'result-display.html'
})
export class ResultDisplayComponent {

  @Input()
  item: WertungContainer;

  @Input()
  title: string;

  constructor() {
  }

}
