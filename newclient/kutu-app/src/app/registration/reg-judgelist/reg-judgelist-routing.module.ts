import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { RegJudgelistPage } from './reg-judgelist.page';

const routes: Routes = [
  {
    path: '',
    component: RegJudgelistPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RegJudgelistPageRoutingModule {}
