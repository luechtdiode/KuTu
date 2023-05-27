import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-wertung-avg-calc',
  templateUrl: './wertung-avg-calc.component.html',
  styleUrls: ['./wertung-avg-calc.component.scss'],
})
export class WertungAvgCalcComponent implements OnInit {

  @Input()
  hidden: boolean;

  @Input()
  readonly: boolean;
  
  @Input()
  waiting: boolean;

  @Input()
  valueTitle: string;

  @Input()
  valueDescription: string;

  @Input()
  singleValue: number;

  @Output()
  avgValue: EventEmitter<number> = new EventEmitter();

  singleValues: {value:number}[] = [];

  ngOnInit() {
    this.singleValues = [{value: this.singleValue}];
  }

  get title(): string {
    return this.valueTitle;
  }

  get singleValueContainer() {
    return this.singleValues[0];
  }

  add() {
    this.singleValues = [...this.singleValues, {value: 0.000}];
  }  

  remove(index) {
    if (index > -1) {
      this.singleValues.splice(index, 1);
    }
  }

  onChange(event, item) {
    item.value = event.target.value;
  }
  onBlur(event, item) {
    item.value = Number(event.target.value).toFixed(3);
  }

  calcAvg(): number {
    const avg1 = this.singleValues
      .filter(item => !!item.value)
      .map(item => Number(item.value))
      .filter(item => item > 0)
    const avg2 = Number((avg1.reduce((sum, current) => sum + current, 0) / avg1.length).toFixed(3));
    this.avgValue.emit(avg2);
    return avg2;
  }
}
