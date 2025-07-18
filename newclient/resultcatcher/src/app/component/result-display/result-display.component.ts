import { Component, Input, OnInit } from '@angular/core';
import { AlertController, IonButton } from '@ionic/angular';
import { WertungContainer } from 'src/app/backend-types';
import { gearMapping, turn10ProgrammNames } from 'src/app/utils';

export enum GroupBy {
  NONE, ATHLET, PROGRAMM
}

@Component({
    selector: 'result-display',
    templateUrl: './result-display.component.html',
    styleUrls: ['./result-display.component.scss'],
    standalone: false
})
export class ResultDisplayComponent implements OnInit {

  groupBy = GroupBy;

  @Input()
  item: WertungContainer;

  @Input()
  title: string;

  @Input()
  groupedBy: GroupBy = GroupBy.NONE;

  get hasDNoteDetails() {
    return this.item.wertung.variables?.dDetails;
  }

  get hasENoteDetails() {
    return this.item.wertung.variables?.eDetails;
  }

  get dNoteDetails() {
    return this.item.wertung.variables?.dVariables.filter(v => v.value > 0).map(v => v.value.toFixed(v.scale)) || [this.item.wertung.noteD?.toFixed(3) || 0].join(', '); 
  }
  
  get eNoteDetails() {
    return this.item.wertung.variables?.eVariables.filter(v => v.value > 0).map(v => v.value.toFixed(v.scale)) || [this.item.wertung.noteE?.toFixed(3) || 0].join(', '); ; 
  }
  
  get pNoteDetails() {
    return this.item.wertung.variables?.pVariables.filter(v => v.value > 0).map(v => v.value.toFixed(v.scale)) || [0]; 
  }
  
  get dNoteLabel() {
    return this.item.isDNoteUsed && turn10ProgrammNames.indexOf(this.item.programm) > -1 ? "A" : "D";
  }

  get eNoteLabel() {
    return this.item.isDNoteUsed && turn10ProgrammNames.indexOf(this.item.programm) > -1 ? "B" : "E";
  }

  get titlestart() {
    return this.title.split(' - ')[0];
  }

  get titlerest() {
    return this.title.split(' - ')[1];
  }

  get gearImage() {
    return gearMapping[this.item.geraet] || undefined;
  }

  constructor(private readonly alertController: AlertController) { }

  ngOnInit() {
    //
  }

  async presentDetails(open: boolean) {
    const text = `
      ${this.dNoteLabel}: [${this.dNoteDetails}],
      ${this.eNoteLabel}: [${this.eNoteDetails}], 
      Penalty: [${this.pNoteDetails}], 
      Streichresultat: [${this.item.isStroked ? 'ja' : 'nein'}]`
    const alert = await this.alertController.create({
      header: `${this.titlerest || this.title} Wertung Details`,
      subHeader: `Total Punkte: [${this.item.wertung.endnote?.toFixed(3) || "Kein Resultat"}]`,
      message: text,
      buttons: ['OK'],
    });

    await alert.present();
  }
}
