(window.webpackJsonp=window.webpackJsonp||[]).push([[20],{Yuue:function(t,e,n){"use strict";n.r(e),n.d(e,"SearchAthletPageModule",(function(){return F}));var i=n("ofXK"),o=n("3Pt+"),r=n("tyNb"),c=n("TEn/"),a=n("mrSG"),b=n("cygB"),s=n("XNiG"),l=n("2Vo4"),u=n("LRne"),p=n("pLZG"),m=n("lJxs"),f=n("Kj3r"),d=n("/uUt"),g=n("w1tV"),h=n("eIep"),v=n("fXoL"),y=function(){function t(){}return t.prototype.ngOnInit=function(){},t.\u0275fac=function(e){return new(e||t)},t.\u0275cmp=v.Bb({type:t,selectors:[["startlist-item"]],inputs:{programm:"programm",teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:16,vars:4,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"]],template:function(t,e){1&t&&(v.Kb(0,"ion-item",0),v.Sb("click",(function(){return e.selected?e.selected.emit(e.teilnehmer):{}})),v.Kb(1,"ion-avatar",1),v.Ib(2,"img",2),v.Jb(),v.Kb(3,"ion-label"),v.kc(4),v.Kb(5,"small"),v.kc(6),v.Jb(),v.Ib(7,"br"),v.Kb(8,"small"),v.Kb(9,"em"),v.kc(10),v.Jb(),v.Jb(),v.Jb(),v.Kb(11,"ion-note",3),v.kc(12," Startger\xe4t:"),v.Ib(13,"br"),v.Kb(14,"b"),v.kc(15),v.Jb(),v.Jb(),v.Jb()),2&t&&(v.xb(4),v.mc("",e.teilnehmer.athlet,"\xa0"),v.xb(2),v.lc(e.teilnehmer.verein+", "+e.programm.programm),v.xb(4),v.lc(e.teilnehmer.durchgang),v.xb(5),v.lc(e.teilnehmer.start))},directives:[c.q,c.d,c.v,c.z],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]}),t}();function k(t,e){if(1&t&&(v.Kb(0,"ion-select-option",9),v.kc(1),v.Vb(2,"date"),v.Jb()),2&t){var n=e.$implicit;v.ac("value",n.uuid),v.xb(1),v.mc(" ",n.titel+" "+v.Xb(2,2,n.datum,"dd-MM-yy"),"")}}function J(t,e){if(1&t){var n=v.Lb();v.Kb(0,"ion-select",7),v.Sb("ngModelChange",(function(t){return v.ec(n),v.Ub(2).competition=t})),v.jc(1,k,3,5,"ion-select-option",8),v.Jb()}if(2&t){var i=v.Ub(2);v.ac("ngModel",i.competition),v.xb(1),v.ac("ngForOf",i.getCompetitions())}}function S(t,e){if(1&t&&(v.Kb(0,"ion-col"),v.Kb(1,"ion-item"),v.Kb(2,"ion-label"),v.kc(3,"Wettkampf"),v.Jb(),v.jc(4,J,2,2,"ion-select",6),v.Jb(),v.Jb()),2&t){var n=v.Ub();v.xb(4),v.ac("ngIf",n.getCompetitions().length>0)}}function K(t,e){if(1&t&&(v.Kb(0,"ion-note",10),v.kc(1),v.Jb()),2&t){var n=v.Ub();v.xb(1),v.mc(" ",n.competitionName()," ")}}function x(t,e){1&t&&(v.Kb(0,"ion-item"),v.Kb(1,"ion-label"),v.kc(2,"loading ..."),v.Jb(),v.Ib(3,"ion-spinner"),v.Jb())}function O(t,e){if(1&t){var n=v.Lb();v.Kb(0,"ion-item-sliding",null,12),v.Kb(2,"startlist-item",13),v.Sb("click",(function(){v.ec(n);var t=e.$implicit,i=v.dc(1);return v.Ub(3).itemTapped(t,i)})),v.Jb(),v.Kb(3,"ion-item-options",14),v.Kb(4,"ion-item-option",15),v.Sb("click",(function(){v.ec(n);var t=e.$implicit,i=v.dc(1);return v.Ub(3).followAthlet(t,i)})),v.Ib(5,"ion-icon",16),v.kc(6),v.Jb(),v.Kb(7,"ion-item-option",17),v.Sb("click",(function(){v.ec(n);var t=e.$implicit,i=v.dc(1);return v.Ub(3).followRiege(t,i)})),v.Ib(8,"ion-icon",16),v.kc(9," Riege verfolgen "),v.Jb(),v.Jb(),v.Jb()}if(2&t){var i=e.$implicit,o=v.Ub().$implicit;v.xb(2),v.ac("teilnehmer",i)("programm",o),v.xb(4),v.mc(" ",i.athlet," verfolgen ")}}function j(t,e){if(1&t&&(v.Kb(0,"div"),v.Kb(1,"ion-item-divider"),v.kc(2),v.Jb(),v.jc(3,O,10,3,"ion-item-sliding",11),v.Jb()),2&t){var n=e.$implicit;v.xb(2),v.lc(n.programm),v.xb(1),v.ac("ngForOf",n.teilnehmer)}}function w(t,e){if(1&t&&(v.Kb(0,"ion-content"),v.Kb(1,"ion-list"),v.jc(2,x,4,0,"ion-item",3),v.Vb(3,"async"),v.jc(4,j,4,2,"div",11),v.Jb(),v.Jb()),2&t){var n=v.Ub();v.xb(2),v.ac("ngIf",v.Wb(3,2,n.isBusy)),v.xb(2),v.ac("ngForOf",n.filteredStartList.programme)}}var C=function(){function t(t,e,n){this.navCtrl=t,this.route=e,this.backendService=n,this.tMyQueryStream=new s.a,this.sFilterTask=void 0,this.busy=new l.a(!1),this.backendService.competitions||this.backendService.getCompetitions()}return t.prototype.ngOnInit=function(){this.busy.next(!0);var t=this.route.snapshot.paramMap.get("wkId");t&&(this.competition=t)},Object.defineProperty(t.prototype,"stationFreezed",{get:function(){return this.backendService.stationFreezed},enumerable:!1,configurable:!0}),Object.defineProperty(t.prototype,"competition",{get:function(){return this.backendService.competition||""},set:function(t){var e=this;this.startlist&&t===this.backendService.competition||(this.busy.next(!0),this.startlist={},this.backendService.getDurchgaenge(t),this.backendService.loadStartlist(void 0).subscribe((function(t){e.startlist=t,e.busy.next(!1);var n=e.tMyQueryStream.pipe(Object(p.a)((function(t){return!!t&&!!t.target&&!!t.target.value})),Object(m.a)((function(t){return t.target.value})),Object(f.a)(1e3),Object(d.a)(),Object(g.a)());n.subscribe((function(t){e.busy.next(!0)})),n.pipe(Object(h.a)(e.runQuery(t))).subscribe((function(t){e.sFilteredStartList=t,e.busy.next(!1)}))})))},enumerable:!1,configurable:!0}),Object.defineProperty(t.prototype,"startlist",{get:function(){return this.sStartList},set:function(t){this.sStartList=t,this.reloadList(this.sMyQuery)},enumerable:!1,configurable:!0}),Object.defineProperty(t.prototype,"filteredStartList",{get:function(){return this.sFilteredStartList||{programme:[]}},enumerable:!1,configurable:!0}),Object.defineProperty(t.prototype,"isBusy",{get:function(){return this.busy},enumerable:!1,configurable:!0}),t.prototype.runQuery=function(t){var e=this;return function(n){var i,o=n.trim();return i={programme:[]},o&&t&&t.programme.forEach((function(n){var r=e.filter(o),c=n.teilnehmer.filter((function(t){return r(t,n.programm)}));if(c.length>0){var b={programm:n.programm,teilnehmer:c};i=Object.assign({},t,{programme:Object(a.e)(i.programme,[b])})}})),Object(u.a)(i)}},t.prototype.reloadList=function(t){this.tMyQueryStream.next(t)},t.prototype.itemTapped=function(t,e){e.getOpenAmount().then((function(t){t>0?e.close():e.open("end")}))},t.prototype.followAthlet=function(t,e){e.close(),this.navCtrl.navigateForward("athlet-view/"+this.backendService.competition+"/"+t.athletid)},t.prototype.followRiege=function(t,e){var n=this;e.close(),this.backendService.getGeraete(this.backendService.competition,t.durchgang).subscribe((function(e){n.backendService.getSteps(n.backendService.competition,t.durchgang,e.find((function(e){return e.name===t.start})).id).subscribe((function(t){n.navCtrl.navigateForward("station")}))}))},t.prototype.getCompetitions=function(){return this.backendService.competitions||[]},t.prototype.competitionName=function(){var t=this;if(!this.backendService.competitions)return"";var e=this.backendService.competitions.filter((function(e){return e.uuid===t.backendService.competition})).map((function(t){return t.titel+", am "+(t.datum+"T").split("T")[0].split("-").reverse().join("-")}));return 1===e.length?(this.startlist||(this.competition=this.backendService.competition),e[0]):""},t.prototype.filter=function(t){var e=t.toUpperCase().split(" ");return function(t,n){return e.filter((function(e){return t.athletid+""===e||t.athlet.toUpperCase().indexOf(e)>-1||t.verein.toUpperCase().indexOf(e)>-1||t.start.toUpperCase().indexOf(e)>-1||t.verein.toUpperCase().indexOf(e)>-1||n.indexOf(e)>-1||void 0})).length===e.length}},t.\u0275fac=function(e){return new(e||t)(v.Hb(c.N),v.Hb(r.a),v.Hb(b.a))},t.\u0275cmp=v.Bb({type:t,selectors:[["app-search-athlet"]],decls:16,vars:4,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingAthletItem",""],[3,"teilnehmer","programm","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["color","secondary",3,"click"]],template:function(t,e){1&t&&(v.Kb(0,"ion-header"),v.Kb(1,"ion-toolbar"),v.Kb(2,"ion-buttons",0),v.Kb(3,"ion-menu-toggle"),v.Kb(4,"ion-button"),v.Ib(5,"ion-icon",1),v.Jb(),v.Jb(),v.Jb(),v.Kb(6,"ion-title"),v.Kb(7,"ion-grid",2),v.Kb(8,"ion-row"),v.Kb(9,"ion-col"),v.Kb(10,"ion-label"),v.kc(11,"Suche Turner/-in"),v.Jb(),v.Jb(),v.jc(12,S,5,1,"ion-col",3),v.Jb(),v.Jb(),v.Jb(),v.jc(13,K,2,1,"ion-note",4),v.Jb(),v.Kb(14,"ion-searchbar",5),v.Sb("ngModelChange",(function(t){return e.sMyQuery=t}))("ionInput",(function(t){return e.reloadList(t)}))("ionCancel",(function(t){return e.reloadList(t)})),v.Jb(),v.Jb(),v.jc(15,w,5,4,"ion-content",3)),2&t&&(v.xb(12),v.ac("ngIf",!e.competition),v.xb(1),v.ac("ngIf",e.competition),v.xb(1),v.ac("ngModel",e.sMyQuery),v.xb(1),v.ac("ngIf",e.competition&&e.startlist&&e.startlist.programme))},directives:[c.n,c.J,c.h,c.y,c.g,c.o,c.I,c.m,c.B,c.j,c.v,i.m,c.C,c.R,o.d,o.g,c.q,c.D,c.Q,i.l,c.E,c.z,c.k,c.w,c.F,c.r,c.u,y,c.t,c.s],pipes:[i.e,i.b],styles:[""]}),t}(),I=n("bDjW"),M=[{path:"",component:C}],F=function(){function t(){}return t.\u0275mod=v.Fb({type:t}),t.\u0275inj=v.Eb({factory:function(e){return new(e||t)},imports:[[i.c,o.a,c.K,r.i.forChild(M),I.a]]}),t}()}}]);