import { Component, OnInit, Output, EventEmitter, input } from '@angular/core';
import { ClubRegistration } from 'src/app/backend-types';

@Component({
    selector: 'clublist-item',
    templateUrl: './clublist-item.component.html',
    styleUrls: ['./clublist-item.component.scss'],
    standalone: false
})
export class ClublistItemComponent implements OnInit {


  readonly clubregistration = input<ClubRegistration>(undefined);

  readonly status = input<string>(undefined);

  @Output()
  selected = new EventEmitter<ClubRegistration>();

  constructor() { }

  ngOnInit() {}
}
