(window.webpackJsonp=window.webpackJsonp||[]).push([[12],{Xmi3:function(e,t,n){"use strict";n.r(t),n.d(t,"LastResultsPageModule",(function(){return y}));var i=n("ofXK"),o=n("3Pt+"),r=n("tyNb"),c=n("TEn/"),a=n("cygB"),s=n("pLZG"),b=n("fXoL"),l=n("H2Ii");function u(e,t){if(1&e&&(b.Kb(0,"ion-select-option",9),b.kc(1),b.Vb(2,"date"),b.Jb()),2&e){var n=t.$implicit;b.ac("value",n.uuid),b.xb(1),b.mc(" ",n.titel+" "+b.Xb(2,2,n.datum,"dd-MM-yy"),"")}}function p(e,t){if(1&e){var n=b.Lb();b.Kb(0,"ion-select",7),b.Sb("ngModelChange",(function(e){return b.ec(n),b.Ub(2).competition=e})),b.jc(1,u,3,5,"ion-select-option",8),b.Jb()}if(2&e){var i=b.Ub(2);b.ac("ngModel",i.competition),b.xb(1),b.ac("ngForOf",i.getCompetitions())}}function g(e,t){if(1&e&&(b.Kb(0,"ion-col"),b.Kb(1,"ion-item"),b.Kb(2,"ion-label"),b.kc(3,"Wettkampf"),b.Jb(),b.jc(4,p,2,2,"ion-select",6),b.Jb(),b.Jb()),2&e){var n=b.Ub();b.xb(4),b.ac("ngIf",n.getCompetitions().length>0)}}function f(e,t){if(1&e&&(b.Kb(0,"ion-note",10),b.kc(1),b.Jb()),2&e){var n=b.Ub();b.xb(1),b.mc(" ",n.competitionName()," ")}}var m=function(e){return{"gradient-border":e}};function d(e,t){if(1&e){var n=b.Lb();b.Kb(0,"ion-col",11),b.Sb("click",(function(e){b.ec(n);var i=t.$implicit;return b.Ub().itemTapped(e,i)})),b.Kb(1,"div",12),b.Ib(2,"result-display",13),b.Jb(),b.Jb()}if(2&e){var i=t.$implicit,o=b.Ub();b.xb(1),b.ac("ngClass",b.bc(3,m,o.isNew(i))),b.xb(1),b.ac("item",i)("title",o.getTitle(i))}}var h=function(){function e(e,t){this.navCtrl=e,this.backendService=t,this.items=[],this.geraete=[],this.backendService.competitions||this.backendService.getCompetitions()}return e.prototype.ngOnInit=function(){var e=this;this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe((function(t){e.geraete=t||[],e.sortItems()})),this.backendService.newLastResults.pipe(Object(s.a)((function(e){return!!e&&!!e.results}))).subscribe((function(t){e.lastItems=e.items.map((function(t){return t.id*e.geraete.length+t.geraet})),e.items=[],Object.keys(t.results).forEach((function(n){e.items.push(t.results[n])})),e.sortItems()}))},e.prototype.sortItems=function(){var e=this;this.items=this.items.sort((function(t,n){var i=t.programm.localeCompare(n.programm);return 0===i&&(i=e.geraetOrder(t.geraet)-e.geraetOrder(n.geraet)),i})).filter((function(e){return void 0!==e.wertung.endnote}))},e.prototype.isNew=function(e){var t=this;return 0===this.lastItems.filter((function(n){return n===e.id*t.geraete.length+e.geraet})).length},Object.defineProperty(e.prototype,"stationFreezed",{get:function(){return this.backendService.stationFreezed},enumerable:!1,configurable:!0}),Object.defineProperty(e.prototype,"competition",{get:function(){return this.backendService.competition||""},set:function(e){var t=this;this.stationFreezed||(this.backendService.getDurchgaenge(e),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe((function(e){t.geraete=e||[],t.sortItems()})))},enumerable:!1,configurable:!0}),e.prototype.getCompetitions=function(){return this.backendService.competitions||[]},e.prototype.competitionName=function(){var e=this;if(!this.backendService.competitions)return"";var t=this.backendService.competitions.filter((function(t){return t.uuid===e.backendService.competition})).map((function(e){return e.titel+", am "+(e.datum+"T").split("T")[0].split("-").reverse().join("-")}));return 1===t.length?t[0]:""},e.prototype.geraetOrder=function(e){return this.geraete?this.geraete.findIndex((function(t){return t.id===e})):0},e.prototype.geraetText=function(e){if(!this.geraete)return"";var t=this.geraete.filter((function(t){return t.id===e})).map((function(e){return e.name}));return 1===t.length?t[0]:""},e.prototype.getTitle=function(e){return e.programm+" - "+this.geraetText(e.geraet)},e.prototype.itemTapped=function(e,t){},e.\u0275fac=function(t){return new(t||e)(b.Hb(c.N),b.Hb(a.a))},e.\u0275cmp=b.Bb({type:e,selectors:[["app-last-results"]],decls:18,vars:3,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",3,"click",4,"ngFor","ngForOf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],["size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",1,"align-self-start",3,"click"],[3,"ngClass"],[3,"item","title"]],template:function(e,t){1&e&&(b.Kb(0,"ion-header"),b.Kb(1,"ion-toolbar"),b.Kb(2,"ion-buttons",0),b.Kb(3,"ion-menu-toggle"),b.Kb(4,"ion-button"),b.Ib(5,"ion-icon",1),b.Jb(),b.Jb(),b.Jb(),b.Kb(6,"ion-title"),b.Kb(7,"ion-grid",2),b.Kb(8,"ion-row"),b.Kb(9,"ion-col"),b.Kb(10,"ion-label"),b.kc(11,"Aktuelle Resultate"),b.Jb(),b.Jb(),b.jc(12,g,5,1,"ion-col",3),b.Jb(),b.Jb(),b.Jb(),b.jc(13,f,2,1,"ion-note",4),b.Jb(),b.Jb(),b.Kb(14,"ion-content"),b.Kb(15,"ion-grid",2),b.Kb(16,"ion-row"),b.jc(17,d,3,5,"ion-col",5),b.Jb(),b.Jb(),b.Jb()),2&e&&(b.xb(12),b.ac("ngIf",!t.competition),b.xb(1),b.ac("ngIf",t.competition),b.xb(4),b.ac("ngForOf",t.items))},directives:[c.n,c.J,c.h,c.y,c.g,c.o,c.I,c.m,c.B,c.j,c.v,i.m,c.k,i.l,c.q,c.D,c.Q,o.d,o.g,c.E,c.z,i.k,l.a],pipes:[i.e],styles:[""]}),e}(),v=n("bDjW"),k=[{path:"",component:h}],y=function(){function e(){}return e.\u0275mod=b.Fb({type:e}),e.\u0275inj=b.Eb({factory:function(t){return new(t||e)},imports:[[i.c,o.a,c.K,r.i.forChild(k),v.a]]}),e}()}}]);