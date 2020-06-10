import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { ClubRegistration } from 'src/app/backend-types';

@Component({
  selector: 'clublist-item',
  templateUrl: './clublist-item.component.html',
  styleUrls: ['./clublist-item.component.scss'],
})
export class ClublistItemComponent implements OnInit {


  @Input()
  clubregistration: ClubRegistration;

  @Output()
  selected: EventEmitter<ClubRegistration>;

  constructor() { }

  ngOnInit() {}
}
