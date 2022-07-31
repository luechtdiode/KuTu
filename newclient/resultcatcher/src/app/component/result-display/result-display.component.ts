import { Component, OnInit, Input } from '@angular/core';
import { WertungContainer } from 'src/app/backend-types';
import { gearMapping } from 'src/app/utils';

export enum GroupBy {
  NONE, ATHLET, PROGRAMM
}

@Component({
  selector: 'result-display',
  templateUrl: './result-display.component.html',
  styleUrls: ['./result-display.component.scss'],
})
export class ResultDisplayComponent implements OnInit {
  groupBy = GroupBy; 

  @Input()
  item: WertungContainer;

  @Input()
  title: string;

  @Input()
  groupedBy: GroupBy = GroupBy.NONE;



  get titlestart() {
    return this.title.split(' - ')[0];
  }

  get titlerest() {
    return this.title.split(' - ')[1];
  }

  get gearImage() {
    return gearMapping[this.item.geraet] || undefined;
  }

  constructor() { }

  ngOnInit() {}

}
