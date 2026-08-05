import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { ScoreCalcPage } from './scorecalc.page';
import { ScorecalcEditorModalComponent } from './scorecalc-editor-modal.component';

const routes: Routes = [{ path: '', component: ScoreCalcPage }];

@NgModule({
  declarations: [ScoreCalcPage, ScorecalcEditorModalComponent],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class ScoreCalcPageModule {}
