(window.webpackJsonp=window.webpackJsonp||[]).push([[14],{Yuue:function(e,t,n){"use strict";n.r(t),n.d(t,"SearchAthletPageModule",(function(){return A}));var i=n("ofXK"),o=n("3Pt+"),r=n("tyNb"),c=n("TEn/"),a=n("mrSG"),s=n("cygB"),u=n("XNiG"),l=n("2Vo4"),b=n("LRne"),p=n("pLZG"),m=n("lJxs"),d=n("Ji7U"),f=n("LK+K"),h=n("1OyB"),g=n("vuIU"),v=n("7o/Q"),y=n("D0XW"),k=function(){function e(t,n){Object(h.a)(this,e),this.dueTime=t,this.scheduler=n}return Object(g.a)(e,[{key:"call",value:function(e,t){return t.subscribe(new S(e,this.dueTime,this.scheduler))}}]),e}(),S=function(e){Object(d.a)(n,e);var t=Object(f.a)(n);function n(e,i,o){var r;return Object(h.a)(this,n),(r=t.call(this,e)).dueTime=i,r.scheduler=o,r.debouncedSubscription=null,r.lastValue=null,r.hasValue=!1,r}return Object(g.a)(n,[{key:"_next",value:function(e){this.clearDebounce(),this.lastValue=e,this.hasValue=!0,this.add(this.debouncedSubscription=this.scheduler.schedule(x,this.dueTime,this))}},{key:"_complete",value:function(){this.debouncedNext(),this.destination.complete()}},{key:"debouncedNext",value:function(){if(this.clearDebounce(),this.hasValue){var e=this.lastValue;this.lastValue=null,this.hasValue=!1,this.destination.next(e)}}},{key:"clearDebounce",value:function(){var e=this.debouncedSubscription;null!==e&&(this.remove(e),e.unsubscribe(),this.debouncedSubscription=null)}}]),n}(v.a);function x(e){e.debouncedNext()}var J=n("/uUt"),K=n("w1tV"),O=n("eIep"),j=n("fXoL"),w=function(){function e(){}return e.prototype.ngOnInit=function(){},e.\u0275fac=function(t){return new(t||e)},e.\u0275cmp=j.Bb({type:e,selectors:[["startlist-item"]],inputs:{programm:"programm",teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:16,vars:4,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"]],template:function(e,t){1&e&&(j.Kb(0,"ion-item",0),j.Sb("click",(function(){return t.selected?t.selected.emit(t.teilnehmer):{}})),j.Kb(1,"ion-avatar",1),j.Ib(2,"img",2),j.Jb(),j.Kb(3,"ion-label"),j.kc(4),j.Kb(5,"small"),j.kc(6),j.Jb(),j.Ib(7,"br"),j.Kb(8,"small"),j.Kb(9,"em"),j.kc(10),j.Jb(),j.Jb(),j.Jb(),j.Kb(11,"ion-note",3),j.kc(12," Startger\xe4t:"),j.Ib(13,"br"),j.Kb(14,"b"),j.kc(15),j.Jb(),j.Jb(),j.Jb()),2&e&&(j.xb(4),j.mc("",t.teilnehmer.athlet,"\xa0"),j.xb(2),j.lc(t.teilnehmer.verein+", "+t.programm.programm),j.xb(4),j.lc(t.teilnehmer.durchgang),j.xb(5),j.lc(t.teilnehmer.start))},directives:[c.p,c.c,c.u,c.y],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]}),e}();function I(e,t){if(1&e&&(j.Kb(0,"ion-select-option",9),j.kc(1),j.Vb(2,"date"),j.Jb()),2&e){var n=t.$implicit;j.ac("value",n.uuid),j.xb(1),j.mc(" ",n.titel+" "+j.Xb(2,2,n.datum,"dd-MM-yy"),"")}}function C(e,t){if(1&e){var n=j.Lb();j.Kb(0,"ion-select",7),j.Sb("ngModelChange",(function(e){return j.ec(n),j.Ub(2).competition=e})),j.jc(1,I,3,5,"ion-select-option",8),j.Jb()}if(2&e){var i=j.Ub(2);j.ac("ngModel",i.competition),j.xb(1),j.ac("ngForOf",i.getCompetitions())}}function M(e,t){if(1&e&&(j.Kb(0,"ion-col"),j.Kb(1,"ion-item"),j.Kb(2,"ion-label"),j.kc(3,"Wettkampf"),j.Jb(),j.jc(4,C,2,2,"ion-select",6),j.Jb(),j.Jb()),2&e){var n=j.Ub();j.xb(4),j.ac("ngIf",n.getCompetitions().length>0)}}function L(e,t){if(1&e&&(j.Kb(0,"ion-note",10),j.kc(1),j.Jb()),2&e){var n=j.Ub();j.xb(1),j.mc(" ",n.competitionName()," ")}}function U(e,t){1&e&&(j.Kb(0,"ion-item"),j.Kb(1,"ion-label"),j.kc(2,"loading ..."),j.Jb(),j.Ib(3,"ion-spinner"),j.Jb())}function F(e,t){if(1&e){var n=j.Lb();j.Kb(0,"ion-item-sliding",null,12),j.Kb(2,"startlist-item",13),j.Sb("click",(function(){j.ec(n);var e=t.$implicit,i=j.dc(1);return j.Ub(3).itemTapped(e,i)})),j.Jb(),j.Kb(3,"ion-item-options",14),j.Kb(4,"ion-item-option",15),j.Sb("click",(function(){j.ec(n);var e=t.$implicit,i=j.dc(1);return j.Ub(3).followAthlet(e,i)})),j.Ib(5,"ion-icon",16),j.kc(6),j.Jb(),j.Kb(7,"ion-item-option",17),j.Sb("click",(function(){j.ec(n);var e=t.$implicit,i=j.dc(1);return j.Ub(3).followRiege(e,i)})),j.Ib(8,"ion-icon",16),j.kc(9," Riege verfolgen "),j.Jb(),j.Jb(),j.Jb()}if(2&e){var i=t.$implicit,o=j.Ub().$implicit;j.xb(2),j.ac("teilnehmer",i)("programm",o),j.xb(4),j.mc(" ",i.athlet," verfolgen ")}}function T(e,t){if(1&e&&(j.Kb(0,"div"),j.Kb(1,"ion-item-divider"),j.kc(2),j.Jb(),j.jc(3,F,10,3,"ion-item-sliding",11),j.Jb()),2&e){var n=t.$implicit;j.xb(2),j.lc(n.programm),j.xb(1),j.ac("ngForOf",n.teilnehmer)}}function V(e,t){if(1&e&&(j.Kb(0,"ion-content"),j.Kb(1,"ion-list"),j.jc(2,U,4,0,"ion-item",3),j.Vb(3,"async"),j.jc(4,T,4,2,"div",11),j.Jb(),j.Jb()),2&e){var n=j.Ub();j.xb(2),j.ac("ngIf",j.Wb(3,2,n.isBusy)),j.xb(2),j.ac("ngForOf",n.filteredStartList.programme)}}var B=function(){function e(e,t,n){this.navCtrl=e,this.route=t,this.backendService=n,this.tMyQueryStream=new u.a,this.sFilterTask=void 0,this.busy=new l.a(!1),this.backendService.competitions||this.backendService.getCompetitions()}return e.prototype.ngOnInit=function(){this.busy.next(!0);var e=this.route.snapshot.paramMap.get("wkId");e&&(this.competition=e)},Object.defineProperty(e.prototype,"stationFreezed",{get:function(){return this.backendService.stationFreezed},enumerable:!1,configurable:!0}),Object.defineProperty(e.prototype,"competition",{get:function(){return this.backendService.competition||""},set:function(e){var t=this;this.startlist&&e===this.backendService.competition||(this.busy.next(!0),this.startlist={},this.backendService.getDurchgaenge(e),this.backendService.loadStartlist(void 0).subscribe((function(e){t.startlist=e,t.busy.next(!1);var n=t.tMyQueryStream.pipe(Object(p.a)((function(e){return!!e&&!!e.target&&!!e.target.value})),Object(m.a)((function(e){return e.target.value})),function(e){var t=arguments.length>1&&void 0!==arguments[1]?arguments[1]:y.a;return function(n){return n.lift(new k(e,t))}}(1e3),Object(J.a)(),Object(K.a)());n.subscribe((function(e){t.busy.next(!0)})),n.pipe(Object(O.a)(t.runQuery(e))).subscribe((function(e){t.sFilteredStartList=e,t.busy.next(!1)}))})))},enumerable:!1,configurable:!0}),Object.defineProperty(e.prototype,"startlist",{get:function(){return this.sStartList},set:function(e){this.sStartList=e,this.reloadList(this.sMyQuery)},enumerable:!1,configurable:!0}),Object.defineProperty(e.prototype,"filteredStartList",{get:function(){return this.sFilteredStartList||{programme:[]}},enumerable:!1,configurable:!0}),Object.defineProperty(e.prototype,"isBusy",{get:function(){return this.busy},enumerable:!1,configurable:!0}),e.prototype.runQuery=function(e){var t=this;return function(n){var i,o=n.trim();return i={programme:[]},o&&e&&e.programme.forEach((function(n){var r=t.filter(o),c=n.teilnehmer.filter((function(e){return r(e,n.programm)}));if(c.length>0){var s={programm:n.programm,teilnehmer:c};i=Object.assign({},e,{programme:Object(a.e)(i.programme,[s])})}})),Object(b.a)(i)}},e.prototype.reloadList=function(e){this.tMyQueryStream.next(e)},e.prototype.itemTapped=function(e,t){t.getOpenAmount().then((function(e){e>0?t.close():t.open("end")}))},e.prototype.followAthlet=function(e,t){t.close(),this.navCtrl.navigateForward("athlet-view/"+this.backendService.competition+"/"+e.athletid)},e.prototype.followRiege=function(e,t){var n=this;t.close(),this.backendService.getGeraete(this.backendService.competition,e.durchgang).subscribe((function(t){n.backendService.getSteps(n.backendService.competition,e.durchgang,t.find((function(t){return t.name===e.start})).id).subscribe((function(e){n.navCtrl.navigateForward("station")}))}))},e.prototype.getCompetitions=function(){return this.backendService.competitions||[]},e.prototype.competitionName=function(){var e=this;if(!this.backendService.competitions)return"";var t=this.backendService.competitions.filter((function(t){return t.uuid===e.backendService.competition})).map((function(e){return e.titel+", am "+(e.datum+"T").split("T")[0].split("-").reverse().join("-")}));return 1===t.length?(this.startlist||(this.competition=this.backendService.competition),t[0]):""},e.prototype.filter=function(e){var t=e.toUpperCase().split(" ");return function(e,n){return t.filter((function(t){return e.athletid+""===t||e.athlet.toUpperCase().indexOf(t)>-1||e.verein.toUpperCase().indexOf(t)>-1||e.start.toUpperCase().indexOf(t)>-1||e.verein.toUpperCase().indexOf(t)>-1||n.indexOf(t)>-1||void 0})).length===t.length}},e.\u0275fac=function(t){return new(t||e)(j.Hb(c.L),j.Hb(r.a),j.Hb(s.a))},e.\u0275cmp=j.Bb({type:e,selectors:[["app-search-athlet"]],decls:16,vars:4,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingAthletItem",""],[3,"teilnehmer","programm","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["color","secondary",3,"click"]],template:function(e,t){1&e&&(j.Kb(0,"ion-header"),j.Kb(1,"ion-toolbar"),j.Kb(2,"ion-buttons",0),j.Kb(3,"ion-menu-toggle"),j.Kb(4,"ion-button"),j.Ib(5,"ion-icon",1),j.Jb(),j.Jb(),j.Jb(),j.Kb(6,"ion-title"),j.Kb(7,"ion-grid",2),j.Kb(8,"ion-row"),j.Kb(9,"ion-col"),j.Kb(10,"ion-label"),j.kc(11,"Suche Turner/-in"),j.Jb(),j.Jb(),j.jc(12,M,5,1,"ion-col",3),j.Jb(),j.Jb(),j.Jb(),j.jc(13,L,2,1,"ion-note",4),j.Jb(),j.Kb(14,"ion-searchbar",5),j.Sb("ngModelChange",(function(e){return t.sMyQuery=e}))("ionInput",(function(e){return t.reloadList(e)}))("ionCancel",(function(e){return t.reloadList(e)})),j.Jb(),j.Jb(),j.jc(15,V,5,4,"ion-content",3)),2&e&&(j.xb(12),j.ac("ngIf",!t.competition),j.xb(1),j.ac("ngIf",t.competition),j.xb(1),j.ac("ngModel",t.sMyQuery),j.xb(1),j.ac("ngIf",t.competition&&t.startlist&&t.startlist.programme))},directives:[c.m,c.H,c.g,c.x,c.f,c.n,c.G,c.l,c.A,c.i,c.u,i.m,c.B,c.P,o.d,o.g,c.p,c.C,c.O,i.l,c.D,c.y,c.j,c.v,c.E,c.q,c.t,w,c.s,c.r],pipes:[i.e,i.b],styles:[""]}),e}(),P=n("bDjW"),Q=[{path:"",component:B}],A=function(){function e(){}return e.\u0275mod=j.Fb({type:e}),e.\u0275inj=j.Eb({factory:function(t){return new(t||e)},imports:[[i.c,o.a,c.I,r.i.forChild(Q),P.a]]}),e}()}}]);