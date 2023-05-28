import { Component, EventEmitter, HostBinding, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-wertung-avg-calc',
  templateUrl: './wertung-avg-calc.component.html',
  styleUrls: ['./wertung-avg-calc.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi:true,
      useExisting: WertungAvgCalcComponent
    }
  ]
})
export class WertungAvgCalcComponent implements ControlValueAccessor { 
  static nextId = 0;

  @HostBinding() id = `avg-calc-input-${WertungAvgCalcComponent.nextId++}`;
  
  _fixed: number = 3;
  
  get fixed(): number {
    return this._fixed;
  }

  @Input()
  set fixed(value: number) {
    this._fixed = value;
    this.calcAvg();
    this.markAsTouched();
  }

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

  avgValue: number;

  singleValues: {value:number}[] = [];

  get title(): string {
    return this.valueTitle;
  }

  get singleValueContainer() {
    return this.singleValues[0]?.value || this.avgValue;
  }

  set singleValueContainer(avgValue: number) {
    this.singleValues = [{value: avgValue}];
    this.calcAvg();
    this.markAsTouched();
  }

  add() {
    if (!this.disabled) {
      this.singleValues = [...this.singleValues, {value: 0.000}];
      this.markAsTouched();
    }
  }  

  remove(index) {
    if (!this.disabled && index > -1) {
      this.singleValues.splice(index, 1);
      this.calcAvg();
      this.markAsTouched();
    }
  }

  calcAvg(): number {
    const avg1 = this.singleValues
      .filter(item => !!item.value)
      .map(item => Number(item.value))
      .filter(item => item > 0)
    const avg2 = Number((avg1.reduce((sum, current) => sum + current, 0) / avg1.length).toFixed(this.fixed));
    if (!this.disabled && this.avgValue !== avg2) {
      this.avgValue = avg2;
      this.onChange(avg2);
      console.log('value updated: ' + avg2);
    }
    return avg2;
  }

  onTouched = () => {};
  
  onChange = (avgValue: number) => {};

  onItemChange(event, item) {
    item.value = event.target.value;
    this.calcAvg();
    this.markAsTouched();
  }
  
  onBlur(event, item) {
    item.value = Number(event.target.value).toFixed(this.fixed);
    this.calcAvg();
    this.markAsTouched();
  }

  touched = false;

  disabled = false;

  writeValue(avgValue: number) {
    console.log('writValue ' + avgValue);
    this.avgValue = avgValue;
    this.singleValues = [{value: avgValue}];
  }

  registerOnChange(onChange: any) {
    this.onChange = onChange;
  }

  registerOnTouched(onTouched: any) {
    this.onTouched = onTouched;
  }

  markAsTouched() {
    if (!this.touched) {
      this.onTouched();
      this.touched = true;
    }
  }

  setDisabledState(disabled: boolean) {
    this.disabled = disabled;
  }

  @ViewChild('noteInput') public noteInput: { setFocus: () => void; };
  
  focused = false;

  setFocus() {
    this.noteInput.setFocus();
  }
}
