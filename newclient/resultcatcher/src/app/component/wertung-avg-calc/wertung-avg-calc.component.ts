import { Component, EventEmitter, HostBinding, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    selector: 'app-wertung-avg-calc',
    templateUrl: './wertung-avg-calc.component.html',
    styleUrls: ['./wertung-avg-calc.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            multi: true,
            useExisting: WertungAvgCalcComponent
        }
    ],
    standalone: false
})
export class WertungAvgCalcComponent implements ControlValueAccessor { 
  static nextId = 0;

  @HostBinding() id = `avg-calc-input-${WertungAvgCalcComponent.nextId++}`;

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
  
  _fixed: number;
  
  get fixed(): number {
    this._fixed = Number(localStorage.getItem(`avg-calc-decimals-${this.valueTitle}${localStorage.getItem('current_competition')}`)) || this._fixed;
    return this._fixed;
  }

  @Input()
  set fixed(value: number) {
    this._fixed = value;
  }

  _compMethod: string = 'avg';
  avgValue: number;

  singleValues: {value:number}[] = [];

  get title(): string {
    return this.valueTitle;
  }

  get methodSymbol(): string {
    if (this._compMethod === 'avg') {
      return 'Ø';
    } else {
      return 'Σ';
    }
  }

  get singleValueContainer() {
    return this.singleValues[0]?.value || this.avgValue;
  }

  set singleValueContainer(avgValue: number) {
    this.singleValues = [{value: avgValue}];
    this.calcAvg();
    this.markAsTouched();
  }

  get compMethod(): string {
    this._compMethod = localStorage.getItem(`comp-method-${this.valueTitle}${localStorage.getItem('current_competition')}`) || this._compMethod;
    return this._compMethod;
  }
  set compMethod(value: string) {
    this._compMethod = value;
    localStorage.setItem(`comp-method-${this.valueTitle}${localStorage.getItem('current_competition')}`, value);
    this.calcAvg();
    this.markAsTouched();    
  }

  addKomma() {
    if (this._fixed < 3) {
      this._fixed += 1;
      localStorage.setItem(`avg-calc-decimals-${this.valueTitle}${localStorage.getItem('current_competition')}`, '' + this._fixed);
      this.calcAvg();
      this.markAsTouched();
    }
  }

  removeKomma() {
    if (this._fixed > 0) {
      this._fixed -= 1;
      localStorage.setItem(`avg-calc-decimals-${this.valueTitle}${localStorage.getItem('current_competition')}`, '' + this._fixed);
      this.calcAvg();
      this.markAsTouched();
    }
  }

  add() {
    if (!this.disabled) {
      this.singleValues = [...this.singleValues, {value: 0.000}];
      if (this.singleValues.length === 1) {
        this.singleValues = [...this.singleValues, {value: 0.000}];
      }
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
      .filter(item => !isNaN(item))
      .filter(item => item > 0)
    if (avg1.length === 0) {
      this.avgValue = undefined;
      this.onChange(undefined);
      //console.log('value updated: ' + undefined);
      return undefined;
    }
    console.log('compMethod: ' + this.compMethod);
    const divider = this.compMethod === 'avg' ? avg1.length : 1;
    console.log('divider: ' + divider);
    const avg2 = Number((avg1.reduce((sum, current) => sum + current, 0) / divider).toFixed(this.fixed));
    if (!this.disabled && this.avgValue !== avg2 && !isNaN(avg2)) {
      this.avgValue = avg2;
      this.onChange(avg2);
      //console.log('value updated: ' + avg2);
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
    //console.log('writValue ' + avgValue);
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
