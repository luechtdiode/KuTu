import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { RegJudgelistPageRoutingModule } from './reg-judgelist-routing.module';

import { RegJudgelistPage } from './reg-judgelist.page';
import { RegJudgeItemComponent } from '../reg-judge-item/reg-judge-item.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    RegJudgelistPageRoutingModule
  ],
  exports: [RegJudgelistPage, RegJudgeItemComponent],
  declarations: [RegJudgelistPage, RegJudgeItemComponent]
})
export class RegJudgelistPageModule {}
