import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { backendUrl } from './utils';
import { TokenInterceptor } from './token-interceptor';
import { toBase64String } from '@angular/compiler/src/output/source_map';

declare var location: any;

@Injectable()
export class BackendService {
  private loggedIn = false;

  competitions;
  durchgaenge;
  geraete;
  steps;
  wertungen;

  constructor(public http: HttpClient, public tokenInterceptor: TokenInterceptor) {
    this.loggedIn = !!localStorage.getItem('auth_token');
    if (this.loggedIn) {
      this.tokenInterceptor.accessToken = localStorage.getItem('auth_token');
    }
  }

  login(username, password) {
    let headers = new HttpHeaders();
    headers.append('Authorization', 'Basic ' + btoa(username + ':' + password));
    this.http.get(backendUrl + 'login', { 
      observe: 'response', 
      headers: headers
    }).subscribe((data) => {
      this.tokenInterceptor.accessToken = data.headers.get('x-access-token');
      localStorage.setItem('auth_token', this.tokenInterceptor.accessToken);
    });
  }

  getCompetitions() {
    this.http.get<any>(backendUrl + 'api/competition').subscribe((data) => {
      this.competitions = data;
    });
  }

  getDurchgaenge(competition) {
    this.durchgaenge = undefined;
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<any>(backendUrl + 'api/competition/' + competition).subscribe((data) => {
      this.durchgaenge = data;
    });
  }

  getGeraete(competition, durchgang) {
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<any>(backendUrl + 'api/competition/' + competition + '/' + durchgang).subscribe((data) => {
      this.geraete = data;
    });
  }

  getSteps(competition, durchgang, geraet) {
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<any>(backendUrl + 'api/competition/' + competition + '/' + durchgang + '/' + geraet).subscribe((data) => {
      this.steps = data;
    });
  }

  getWertungen(competition, durchgang, geraet, step) {
    this.wertungen = undefined;
    this.http.get<any>(backendUrl + 'api/competition/' + competition + '/' + durchgang + '/' + geraet + '/' + step).subscribe((data) => {
      this.wertungen = data;
    });
  }
}