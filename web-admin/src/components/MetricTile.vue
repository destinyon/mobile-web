<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
  caption?: string;
}>();
</script>

<template>
  <section class="metric-tile">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small v-if="caption">{{ caption }}</small>
  </section>
</template>
