import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { Backdrop } from 'ionic-angular/components/backdrop/backdrop';
import { BackendService } from '../../app/backend.service';
import { Wettkampf, Geraet, WertungContainer } from '../../app/backend-types';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  _competition: string;
  set competition(competitionId: string) {
    this._competition = competitionId;
    this._durchgang = undefined;
    this._geraet = undefined;
    this._step = undefined;
    
    this.backendService.getDurchgaenge(competitionId);
  }
  get competition(): string {
    return this._competition;
  }

  _durchgang: string;
  set durchgang(d: string) {
    this._durchgang = d;
    this._geraet = undefined;
    this._step = undefined;
    this.backendService.getGeraete(this._competition, d);
  }
  get durchgang(): string {
    return this._durchgang;
  }

  _geraet: number;
  set geraet(geraetId: number) {
    this._geraet = geraetId;
    this.step = '1';
    this.backendService.getSteps(this._competition, this._durchgang, geraetId);
  }
  get geraet(): number {
    return this._geraet;
  }

  _step;
  set step(s) {
    this._step = s;
    this.backendService.getWertungen(this._competition, this._durchgang, this._geraet, s);
  }
  get step() {
    return this._step;
  }

  constructor(public navCtrl: NavController, public backendService: BackendService) {
    this.backendService.getCompetitions();
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }

  getDurchgaenge(): string[] {
    return this.backendService.durchgaenge;
  }

  getGeraete(): Geraet[] {
    return this.backendService.geraete;
  }
  
  getSteps(): string[] {
    return this.backendService.steps;
  }  

  getWertungen(): WertungContainer[] {
    return this.backendService.wertungen;
  }
}
