<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { Permission } from '../data/customer'

export type ProfilePermissions = {
  profileId: string
  name: string
  companies: Array<{
    companyId: string
    companyName: string
    permissions: Permission[]
  }>
}

const props = defineProps<{
  profile: ProfilePermissions
}>()

const emit = defineEmits<{
  close: []
  savePermissions: [{ profileId: string; companyId: string; permissions: Permission[] }]
}>()

const allPermissions: Permission[] = [
  'READ_INFORMATION',
  'APPROVE_CASES',
  'ADD_EMPLOYEES',
  'CHANGE_SALARY',
  'REGISTER_LEAVE_OF_ABSENCE',
  'TERMINATE_EMPLOYMENT',
]

const permissionLabel: Record<Permission, string> = {
  READ_INFORMATION: 'Läs information',
  APPROVE_CASES: 'Godkänn ärenden',
  ADD_EMPLOYEES: 'Lägg till medarbetare',
  CHANGE_SALARY: 'Ändra lön',
  REGISTER_LEAVE_OF_ABSENCE: 'Registrera tjänstledighet',
  TERMINATE_EMPLOYMENT: 'Avsluta anställning',
}

const presetList = [
  { key: 'full', label: 'Full behörighet', permissions: allPermissions },
  { key: 'readonly', label: 'Läsbehörighet', permissions: ['READ_INFORMATION'] as Permission[] },
  {
    key: 'manager',
    label: 'Chef',
    permissions: ['READ_INFORMATION', 'APPROVE_CASES', 'ADD_EMPLOYEES'] as Permission[],
  },
  {
    key: 'hr',
    label: 'HR',
    permissions: [
      'READ_INFORMATION',
      'ADD_EMPLOYEES',
      'REGISTER_LEAVE_OF_ABSENCE',
      'TERMINATE_EMPLOYMENT',
    ] as Permission[],
  },
  { key: 'finance', label: 'Lön', permissions: ['READ_INFORMATION', 'CHANGE_SALARY'] as Permission[] },
  { key: 'clear', label: 'Rensa', permissions: [] as Permission[] },
]

const selectedCompanyId = ref<string | null>(props.profile.companies[0]?.companyId ?? null)
const dialog = ref<HTMLElement | null>(null)

watch(
  () => props.profile.profileId,
  () => {
    selectedCompanyId.value = props.profile.companies[0]?.companyId ?? null
  },
)

const activeCompany = computed(
  () =>
    props.profile.companies.find((c) => c.companyId === selectedCompanyId.value) ??
    props.profile.companies[0] ??
    null,
)

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  dialog.value?.focus()
})

onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))

function togglePermission(companyId: string, permission: Permission): void {
  const company = props.profile.companies.find((c) => c.companyId === companyId)
  if (!company) return
  const updated = company.permissions.includes(permission)
    ? company.permissions.filter((p) => p !== permission)
    : [...company.permissions, permission]
  emit('savePermissions', { profileId: props.profile.profileId, companyId, permissions: updated })
}

function applyPreset(permissions: Permission[], companyId: string): void {
  emit('savePermissions', {
    profileId: props.profile.profileId,
    companyId,
    permissions: [...permissions],
  })
}
</script>

<template>
  <div class="dialog-backdrop" @click.self="emit('close')">
    <section
      ref="dialog"
      class="permissions-dialog"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="`perm-title-${profile.profileId}`"
      tabindex="-1"
    >
      <div class="dialog-header">
        <strong :id="`perm-title-${profile.profileId}`" class="dialog-name">
          {{ profile.name }}
        </strong>
        <button class="close-icon-btn" type="button" aria-label="Stäng" @click="emit('close')">
          ✕
        </button>
      </div>

      <!-- Company tabs (only when >1 company) -->
      <div v-if="profile.companies.length > 1" class="company-tabs">
        <button
          v-for="company in profile.companies"
          :key="company.companyId"
          class="company-tab"
          type="button"
          :class="{ 'is-active': company.companyId === activeCompany?.companyId }"
          @click="selectedCompanyId = company.companyId"
        >
          {{ company.companyName }}
        </button>
      </div>

      <div v-if="activeCompany" class="dialog-body">
        <div class="presets">
          <span class="presets-label">Snabbval:</span>
          <button
            v-for="preset in presetList"
            :key="preset.key"
            class="preset-chip"
            type="button"
            @click="applyPreset(preset.permissions, activeCompany.companyId)"
          >
            {{ preset.label }}
          </button>
        </div>

        <div class="permission-list">
          <label v-for="perm in allPermissions" :key="perm" class="permission-row">
            <input
              type="checkbox"
              :checked="activeCompany.permissions.includes(perm)"
              @change="togglePermission(activeCompany.companyId, perm)"
            />
            {{ permissionLabel[perm] }}
          </label>
        </div>
      </div>

      <div class="dialog-footer">
        <button class="button secondary" type="button" @click="emit('close')">Stäng</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgb(16 42 67 / 42%);
}

.permissions-dialog {
  width: min(100%, 480px);
  max-height: min(700px, calc(100vh - 48px));
  overflow: auto;
  background: var(--surface);
  border: var(--panel-border, 1px solid var(--border));
  border-radius: var(--radius-panel);
  box-shadow: 0 20px 60px rgb(16 42 67 / 20%);
  outline: none;
  display: flex;
  flex-direction: column;
}

/* Header */
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  flex-shrink: 0;
}

.dialog-name {
  font-size: 1rem;
  font-weight: 700;
  color: var(--ink);
}

.close-icon-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--muted);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-icon-btn:hover {
  background: var(--border);
  color: var(--ink);
}

/* Company tabs */
.company-tabs {
  display: flex;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  overflow-x: auto;
  scrollbar-width: none;
  flex-shrink: 0;
}

.company-tabs::-webkit-scrollbar {
  display: none;
}

.company-tab {
  flex-shrink: 0;
  padding: 10px 14px;
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition:
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
}

.company-tab:hover {
  color: var(--ink);
}

.company-tab.is-active {
  color: var(--accent, #0057b7);
  border-bottom-color: var(--accent, #0057b7);
  font-weight: 600;
}

/* Body */
.dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.presets {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.presets-label {
  font-size: 0.75rem;
  color: var(--muted);
  white-space: nowrap;
}

.preset-chip {
  padding: 3px 8px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  font-size: 0.75rem;
  cursor: pointer;
  color: var(--ink);
  transition: background 0.15s;
}

.preset-chip:hover {
  background: color-mix(in srgb, var(--accent, #0057b7) 10%, transparent);
  border-color: var(--accent, #0057b7);
  color: var(--accent, #0057b7);
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.permission-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 2px 0;
}

.permission-row input[type='checkbox'] {
  cursor: pointer;
  accent-color: var(--accent, #0057b7);
}

/* Footer */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

@media (max-width: 520px) {
  .dialog-backdrop {
    padding: 12px;
    align-items: flex-end;
  }

  .permissions-dialog {
    width: 100%;
    max-height: 85vh;
  }

  .permission-row {
    padding: 6px 0;
    font-size: 0.9rem;
  }

  .permission-row input[type='checkbox'] {
    width: 18px;
    height: 18px;
  }

  .preset-chip {
    padding: 5px 10px;
    font-size: 0.8rem;
  }
}
</style>
