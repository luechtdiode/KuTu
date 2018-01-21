import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { Backdrop } from 'ionic-angular/components/backdrop/backdrop';
import { BackendService } from '../../app/backend.service';

@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  _competition;
  set competition(c) {
    this._competition = c;
    this._durchgang = undefined;
    this._geraet = undefined;
    this._step = undefined;
    
    this.backendService.getDurchgaenge(c);
  }
  get competition() {
    return this._competition;
  }

  _durchgang;
  set durchgang(d) {
    this._durchgang = d;
    this._geraet = undefined;
    this._step = undefined;
    this.backendService.getGeraete(this._competition, d);
  }
  get durchgang() {
    return this._durchgang;
  }

  _geraet;
  set geraet(g) {
    this._geraet = g;
    this.step = '1';
    this.backendService.getSteps(this._competition, this._durchgang, g);
  }
  get geraet() {
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

  getCompetitions() {
    return this.backendService.competitions;
  }

  getDurchgaenge() {
    return this.backendService.durchgaenge;
  }

  getGeraete() {
    return this.backendService.geraete;
  }
  
  getSteps() {
    return this.backendService.steps;
  }  

  getWertungen() {
    return this.backendService.wertungen;
  }
}
