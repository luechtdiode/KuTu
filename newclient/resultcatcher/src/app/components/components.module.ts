import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResultDisplayComponent } from '../component/result-display/result-display.component';
import { RiegeListComponent } from '../component/riege-list/riege-list.component';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@NgModule({
  declarations: [ResultDisplayComponent, RiegeListComponent],
  exports: [ResultDisplayComponent, RiegeListComponent],
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
  ]
})
export class ComponentsModule { }
