import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { By } from '@angular/platform-browser';

import { WertungAvgCalcComponent } from './wertung-avg-calc.component';

@Component({
  selector: 'test-host',
  standalone: false,
  template: `
    <form>
      <app-wertung-avg-calc [waiting]="false" [valueTitle]="'E'" [fixed]="3"
        name="noteE" [(ngModel)]="model" [disabled]="false">
      </app-wertung-avg-calc>
    </form>
  `
})
class TestHostComponent {
  model: number = undefined;
}

@Component({
  selector: 'collision-host',
  standalone: false,
  template: `
    <form>
      <app-wertung-avg-calc [waiting]="false" [valueTitle]="'Sprung'" [fixed]="3"
        name="Sprung0" [(ngModel)]="model">
      </app-wertung-avg-calc>
    </form>
  `
})
class CollisionHostComponent {
  model: number = undefined;
}

@Component({
  selector: 'standalone-host',
  standalone: false,
  template: `
    <form>
      <app-wertung-avg-calc name="noteD" [waiting]="false" [valueTitle]="'D'" [fixed]="1"
        [(ngModel)]="dNote" [disabled]="false">
      </app-wertung-avg-calc>
      <app-wertung-avg-calc name="noteE" [waiting]="false" [valueTitle]="'E'" [fixed]="3"
        [(ngModel)]="eNote" [disabled]="false">
      </app-wertung-avg-calc>
    </form>
  `
})
class StandaloneHostComponent {
  dNote: number = undefined;
  eNote: number = undefined;
}

describe('WertungAvgCalcComponent', () => {
  let component: WertungAvgCalcComponent;
  let fixture: ComponentFixture<WertungAvgCalcComponent>;

  describe('isolated', () => {
    beforeEach(async () => {
      TestBed.configureTestingModule({
        declarations: [ WertungAvgCalcComponent ],
        imports: [IonicModule.forRoot(), FormsModule],
        providers: [provideZonelessChangeDetection()]
      }).compileComponents();

      fixture = TestBed.createComponent(WertungAvgCalcComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });

  describe('embedded in form (add-value flow)', () => {
    let hostFixture: ComponentFixture<TestHostComponent>;
    let host: TestHostComponent;
    let avgCalc: WertungAvgCalcComponent;

    beforeEach(async () => {
      TestBed.configureTestingModule({
        declarations: [WertungAvgCalcComponent, TestHostComponent],
        imports: [IonicModule.forRoot(), FormsModule],
        providers: [provideZonelessChangeDetection()]
      }).compileComponents();
      hostFixture = TestBed.createComponent(TestHostComponent);
      host = hostFixture.componentInstance;
      host.model = 8.5;
      hostFixture.detectChanges();
      const el = hostFixture.debugElement.query(By.directive(WertungAvgCalcComponent));
      avgCalc = el.componentInstance;
    });

    it('renders single-value mode initially', () => {
      const singleInput = hostFixture.debugElement.query(By.css('ion-input[name="noteInput"]'));
      expect(singleInput).toBeTruthy();
    });

    it('keeps the component visible after adding another value', () => {
      avgCalc.add();
      hostFixture.detectChanges();

      const rows = hostFixture.debugElement.queryAll(By.css('ion-row.table-row'));
      expect(rows.length).toBe(2);
      const rootItem = hostFixture.debugElement.query(By.css('ion-item'));
      expect(rootItem).toBeTruthy();
      expect(avgCalc.singleValues().length).toBe(2);
    });

    it('recalculates and keeps state when values are entered after adding', () => {
      avgCalc.add();
      hostFixture.detectChanges();
      const values = avgCalc.singleValues();
      values[0].value = 8.5;
      values[1].value = 9.0;
      expect(avgCalc.calcAvg()).toBe(8.75);
    });
  });

  describe('variable-block mode (inner name collides with outer CVA name)', () => {
    let hostFixture: ComponentFixture<CollisionHostComponent>;
    let avgCalc: WertungAvgCalcComponent;

    beforeEach(async () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        declarations: [WertungAvgCalcComponent, CollisionHostComponent],
        imports: [IonicModule.forRoot(), FormsModule],
        providers: [provideZonelessChangeDetection()]
      }).compileComponents();
      hostFixture = TestBed.createComponent(CollisionHostComponent);
      hostFixture.componentInstance.model = 8.5;
      hostFixture.detectChanges();
      const el = hostFixture.debugElement.query(By.directive(WertungAvgCalcComponent));
      avgCalc = el.componentInstance;
    });

    it('stays rendered after adding a value despite name collision', () => {
      avgCalc.add();
      expect(() => hostFixture.detectChanges()).not.toThrow();
      const rootItem = hostFixture.debugElement.query(By.css('ion-item'));
      expect(rootItem).toBeTruthy();
      const rows = hostFixture.debugElement.queryAll(By.css('ion-row.table-row'));
      expect(rows.length).toBe(2);
    });
  });

  describe('standalone D/E-note mode (real wertung-editor layout, real clicks)', () => {
    let hostFixture: ComponentFixture<StandaloneHostComponent>;
    let eCalc: WertungAvgCalcComponent;

    beforeEach(async () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        declarations: [WertungAvgCalcComponent, StandaloneHostComponent],
        imports: [IonicModule.forRoot(), FormsModule],
        providers: [provideZonelessChangeDetection()]
      }).compileComponents();
      hostFixture = TestBed.createComponent(StandaloneHostComponent);
      hostFixture.componentInstance.eNote = 8.5;
      hostFixture.detectChanges();
      const calcs = hostFixture.debugElement.queryAll(By.directive(WertungAvgCalcComponent));
      eCalc = calcs[1].componentInstance;
    });

    it('E-note stays rendered when the add button is clicked', () => {
      const calcsBefore = hostFixture.debugElement.queryAll(By.directive(WertungAvgCalcComponent));
      const addButtons = calcsBefore[1].queryAll(By.css('ion-button'))
        .filter(b => b.nativeElement.getAttribute('ng-reflect-no-router-link') === null);
      expect(addButtons.length).toBeGreaterThan(0);
      addButtons[0].nativeElement.click();
      expect(() => hostFixture.detectChanges()).not.toThrow();

      const calcs = hostFixture.debugElement.queryAll(By.directive(WertungAvgCalcComponent));
      expect(calcs.length).toBe(2);
      const rows = hostFixture.debugElement.queryAll(By.css('ion-row.table-row'));
      expect(rows.length).toBe(2);
      expect(eCalc.singleValues().length).toBe(2);
    });

    it('both instances keep their values independently', () => {
      eCalc.add();
      hostFixture.detectChanges();
      hostFixture.componentInstance.dNote = 7.0;
      hostFixture.detectChanges();
      const calcs = hostFixture.debugElement.queryAll(By.directive(WertungAvgCalcComponent));
      expect(calcs[1].componentInstance.singleValues().length).toBe(2);
      expect(hostFixture.debugElement.queryAll(By.css('ion-row.table-row')).length).toBe(2);
    });
  });
});
