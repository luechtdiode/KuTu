import { Component, OnInit, Input } from '@angular/core';
import { WertungContainer } from 'src/app/backend-types';

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

  gearMapping = {
    1: "boden.svg",
    2: "pferdpauschen.svg",
    3: "ringe.svg",
    4: "sprung.svg",
    5: "barren.svg",
    6: "reck.svg",
    26: "ringe.svg",
    27: "stufenbarren.svg",
    28: "schwebebalken.svg",
    29: "minitramp"
  };

  get titlestart() {
    return this.title.split(' - ')[0];
  }

  get titlerest() {
    return this.title.split(' - ')[1];
  }

  get gearImage() {
    return this.gearMapping[this.item.geraet] || undefined;
  }

  constructor() { }

  ngOnInit() {}

}
