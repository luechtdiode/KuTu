import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { CreateCompetitionPage } from './create-competition.page';
import { AltersklassenEditorComponent } from '../editors/altersklassen-editor.component';
import { RiegenRotationsregelEditorComponent } from '../editors/riegenrotationsregel-editor.component';
import { PunktegleichstandsregelEditorComponent } from '../editors/punktegleichstandsregel-editor.component';
import { TeamregelEditorComponent } from '../editors/teamregel-editor.component';

const routes: Routes = [{ path: '', component: CreateCompetitionPage }];

@NgModule({
  declarations: [
    CreateCompetitionPage,
    AltersklassenEditorComponent,
    RiegenRotationsregelEditorComponent,
    PunktegleichstandsregelEditorComponent,
    TeamregelEditorComponent
  ],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class CreateCompetitionPageModule {}
